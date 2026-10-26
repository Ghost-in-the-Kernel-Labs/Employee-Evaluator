package evaluator.ingest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import evaluator.rubric.Evidence;
import evaluator.rubric.Fact;
import evaluator.rubric.Metric;
import evaluator.rubric.State;
import evaluator.rubric.Subject;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * The card's rules for the judged kind (TODO.md 2.2k, E-056, E-057, E-082, E-086): worked on small hand-written judgments and records, no files. The
 * record's counts are E-084's: with 25, 24, 29, 27 of 30 every field is live; with 22, 24, 28, 26 none of the three that matter is.
 */
class JudgedRulesTest {
    private static final String HASH = AgreementRecordTest.HASH;
    private static final AgreementRecord LIVE = AgreementRecordTest.parsed(25, 24, 29, 27);
    private static final AgreementRecord NOT_LIVE = AgreementRecordTest.parsed(22, 24, 28, 26);
    private static final List<Fact> MEASURED = List.of(measured("SIZE.source_lines", 22), measured("SIZE.source_files", 5), measured("COMPLEXITY.dependency_depth", 2),
            measured("MAINTAINABILITY.duplication", 0), measured("NAVIGABILITY.checklist", 40), measured("QUALITY.tests.pass_rate", 1.0), measured("PROVENANCE.history.spread_days", 9));

    private static Fact measured(String kind, Object value) {
        return Fact.measured(kind, Subject.repository(), value, new Evidence("a tool 1", List.of("src/a.py")));
    }

    private static Judgment judgment(String kind, String ambition, int quality, String review) {
        return Judgment.read("""
                {"promptHash": "%s", "models": ["m-1", "m-2"], "judgment": {"kind": "%s", "folders": [],
                 "scores": {"quality": %s, "complexity": {}, "navigability": {}}, "ambition": %s, "quality": %d, "verdict": "ok"}}"""
                .formatted(HASH, kind, review, ambition, quality));
    }

    private static Judgment judgment(String kind) {
        return judgment(kind, "{\"tier\": \"utility\", \"confidence\": 0.9, \"evidence\": [\"src/a.py\"]}", 4, "{\"score\": 70, \"confidence\": 0.99, \"evidence\": [\"src/b.py\"]}");
    }

    private static Fact find(JudgedRules.Outcome outcome, String kind) {
        return outcome.facts().stream().filter(f -> f.kind().equals(kind)).findFirst().orElse(null);
    }

    private static List<String> kinds(List<Fact> facts) {
        return facts.stream().map(Fact::kind).toList();
    }

    /** Generated and the kind live: declined, no facts, the reason in the notes (E-056). */
    @Test
    void generatedIsDeclined() {
        JudgedRules.Outcome outcome = JudgedRules.apply(judgment("generated"), LIVE, MEASURED);
        assertTrue(outcome.declined());
        assertTrue(outcome.facts().isEmpty());
        assertEquals(List.of("kind: generated, declined (agreement 27 of 30)"), outcome.notes());
    }

    /** A kind that is not live is not acted on: a generated repository is neither declined nor withheld from, and the card says so (SC-05, E-082). */
    @Test
    void aKindThatIsNotLiveDecidesNothing() {
        for (String kind : List.of("generated", "plan only", "clone", "built", "mixed", "own-work")) {
            JudgedRules.Outcome outcome = JudgedRules.apply(judgment(kind), NOT_LIVE, MEASURED);
            assertFalse(outcome.declined(), kind);
            assertEquals(MEASURED, outcome.facts(), kind + ": no judged fact, nothing withheld");
            assertEquals(List.of("kind: unverified, not acted on (26 of 30 labels, 27 needed, the constant answer matches 23)",
                    "ambition: unverified (22 of 30 labels, 24 needed, the constant answer matches 14)",
                    "quality: unverified (28 of 30 labels, 29 needed, the constant answer matches 27)"), outcome.notes(), kind);
        }
    }

    /** A plan carries no Size, Complexity or Maintainability and its Ambition reads unverified even when live; every other fact stays, Quality's review too (E-057). */
    @Test
    void aPlanOnlyWithholdsThreeMetricsAndAmbition() {
        JudgedRules.Outcome outcome = JudgedRules.apply(judgment("plan only"), LIVE, MEASURED);
        assertFalse(outcome.declined());
        assertEquals(List.of("NAVIGABILITY.checklist", "QUALITY.tests.pass_rate", "PROVENANCE.history.spread_days", "QUALITY.agent_review"), kinds(outcome.facts()));
        assertEquals("kind: plan only (agreement 27 of 30): Size, Complexity and Maintainability are withheld", outcome.notes().get(0));
        assertEquals("ambition: unverified (a plan only repository, E-057)", outcome.notes().get(1));
        assertEquals(3, outcome.notes().size());
        assertEquals(java.util.Set.of(Metric.SIZE, Metric.COMPLEXITY, Metric.MAINTAINABILITY), JudgedRules.WITHHELD_FOR_A_PLAN);
    }

    /** Clone, built, mixed and own-work are scored as measured, the kind named in a note; clone's says a clone call alone is no decline (E-082, E-086). */
    @Test
    void theOtherKindsAreScoredAsMeasured() {
        for (String kind : List.of("clone", "built", "mixed", "own-work")) {
            JudgedRules.Outcome outcome = JudgedRules.apply(judgment(kind), LIVE, MEASURED);
            assertFalse(outcome.declined(), kind);
            assertEquals(MEASURED, outcome.facts().subList(0, MEASURED.size()), kind);
            assertEquals(MEASURED.size() + 2, outcome.facts().size(), kind + ": the Ambition and the Quality");
            String note = outcome.notes().get(0);
            assertTrue(note.startsWith("kind: " + kind + " (agreement 27 of 30)"), note);
            assertEquals(kind.equals("clone"), note.contains("a clone call alone is no decline (E-082)"), note);
        }
    }

    /** The two judged facts: kinds, values, the evidence naming the prompt and the models, the sources the judge named, the state inferred (SC-02, SC-05). */
    @Test
    void theJudgedFactsCarryTheirEvidence() {
        JudgedRules.Outcome outcome = JudgedRules.apply(judgment("own-work"), LIVE, List.of());
        Fact ambition = find(outcome, "AMBITION.tier"), quality = find(outcome, "QUALITY.agent_review");
        assertEquals("utility", ambition.value());
        assertEquals(4, quality.value());
        assertEquals(Subject.repository(), ambition.subject());
        assertEquals(State.INFERRED, quality.state());
        assertEquals(new Evidence("judge prompt " + HASH + " (m-1, m-2)", List.of("src/a.py")), ambition.evidence());
        assertEquals(List.of("src/b.py"), quality.evidence().sources());
        assertEquals("ambition: utility, confidence 0.833 (the judge claimed 0.9, agreement 25 of 30)", outcome.notes().get(1));
        assertEquals("quality: 4, confidence 0.967 (the judge claimed 0.99, agreement 29 of 30)", outcome.notes().get(2));
    }

    /** A confidence is the judge's own, at most the recorded agreement of its field (SC-05): above it, the agreement; below it, the judge's. Quality is gated by the within-one figure. */
    @Test
    void aConfidenceIsCappedByTheRecordedAgreement() {
        Fact capped = find(JudgedRules.apply(judgment("own-work"), LIVE, List.of()), "AMBITION.tier");
        assertEquals(25 / 30.0, capped.confidence());
        Fact quality = find(JudgedRules.apply(judgment("own-work"), LIVE, List.of()), "QUALITY.agent_review");
        assertEquals(29 / 30.0, quality.confidence(), "0.99 claimed, 29 of 30 recorded");
        Fact modest = find(JudgedRules.apply(judgment("own-work", "{\"tier\": \"toy\", \"confidence\": 0.5, \"evidence\": [\"a\"]}", 2,
                "{\"confidence\": 0.25, \"evidence\": [\"b\"]}"), LIVE, List.of()), "AMBITION.tier");
        assertEquals(0.5, modest.confidence());
        assertEquals(0.25, find(JudgedRules.apply(judgment("own-work", "{\"tier\": \"toy\", \"confidence\": 0.5, \"evidence\": [\"a\"]}", 2,
                "{\"confidence\": 0.25, \"evidence\": [\"b\"]}"), LIVE, List.of()), "QUALITY.agent_review").confidence());
        assertEquals(1.0, JudgedRules.apply(judgment("own-work", "{\"tier\": \"toy\", \"confidence\": 1, \"evidence\": [\"a\"]}", 2, "{}"),
                AgreementRecordTest.parsed(30, 30, 30, 30), List.of()).facts().get(0).confidence(), "a perfect record and a sure judge: 1");
    }

    /** Each judged fact is gated by its own field: Quality exactly is not read, Quality within one gates Quality, Ambition gates Ambition, and the kind gates neither. */
    @Test
    void eachFieldGatesItsOwnFact() {
        JudgedRules.Outcome noAmbition = JudgedRules.apply(judgment("own-work"), AgreementRecordTest.parsed(23, 24, 29, 27), List.of());
        assertEquals(List.of("QUALITY.agent_review"), kinds(noAmbition.facts()));
        assertEquals("ambition: unverified (23 of 30 labels, 24 needed, the constant answer matches 14)", noAmbition.notes().get(1));
        JudgedRules.Outcome noQuality = JudgedRules.apply(judgment("own-work"), AgreementRecordTest.parsed(25, 30, 28, 27), List.of());
        assertEquals(List.of("AMBITION.tier"), kinds(noQuality.facts()), "quality exactly at 30 of 30 does not stand in for within one");
        JudgedRules.Outcome noKind = JudgedRules.apply(judgment("own-work"), AgreementRecordTest.parsed(25, 24, 29, 26), List.of());
        assertEquals(List.of("AMBITION.tier", "QUALITY.agent_review"), kinds(noKind.facts()));
        assertTrue(noKind.notes().get(0).startsWith("kind: unverified"));
        JudgedRules.Outcome exactOnly = JudgedRules.apply(judgment("own-work"), AgreementRecordTest.parsed(25, 23, 29, 27), List.of());
        assertEquals(2, exactOnly.facts().size(), "quality exactly under its mark changes nothing");
    }

    /** An answer whose Ambition or Quality review is not whole (the harness's schema check should have refused it) gives no fact and says why; the rest stands. */
    @Test
    void anAnswerThatIsNotWholeGivesNoFact() {
        String[] ambitions = {"{}", "{\"tier\": \"huge\", \"confidence\": 0.5, \"evidence\": [\"a\"]}", "{\"tier\": \"Toy\", \"confidence\": 0.5, \"evidence\": [\"a\"]}",
                "{\"tier\": \"toy\", \"confidence\": 1.5, \"evidence\": [\"a\"]}", "{\"tier\": \"toy\", \"confidence\": -0.1, \"evidence\": [\"a\"]}",
                "{\"tier\": \"toy\", \"confidence\": \"high\", \"evidence\": [\"a\"]}", "{\"tier\": \"toy\", \"confidence\": 0.5, \"evidence\": []}",
                "{\"tier\": \"toy\", \"confidence\": 0.5, \"evidence\": [\" \"]}", "{\"tier\": \"toy\", \"confidence\": 0.5, \"evidence\": [3]}",
                "{\"tier\": \"toy\", \"confidence\": 0.5, \"evidence\": \"a\"}", "{\"tier\": \"toy\", \"confidence\": 0.5}"};
        for (String ambition : ambitions) {
            JudgedRules.Outcome outcome = JudgedRules.apply(judgment("own-work", ambition, 3, "{\"confidence\": 0.5, \"evidence\": [\"a\"]}"), LIVE, List.of());
            assertEquals(List.of("QUALITY.agent_review"), kinds(outcome.facts()), ambition);
            assertTrue(outcome.notes().get(1).startsWith("ambition: unverified (the judge's answer does not hold a whole ambition"), ambition + " " + outcome.notes());
        }
        JudgedRules.Outcome noReview = JudgedRules.apply(judgment("own-work", "{\"tier\": \"toy\", \"confidence\": 0.5, \"evidence\": [\"a\"]}", 3, "7"), LIVE, List.of());
        assertEquals(List.of("AMBITION.tier"), kinds(noReview.facts()));
        assertTrue(noReview.notes().get(2).startsWith("quality: unverified (the judge's answer does not hold a whole quality"), noReview.notes().toString());
    }

    /** A confidence of exactly 0 or exactly 1 is a confidence; a fact claiming 0 stays at 0. */
    @Test
    void theEdgesOfAConfidenceAreAccepted() {
        for (double edge : new double[] {0, 1}) {
            JudgedRules.Outcome outcome = JudgedRules.apply(judgment("built", "{\"tier\": \"toy\", \"confidence\": " + (int) edge + ", \"evidence\": [\"a\"]}", 3, "{}"), LIVE, List.of());
            assertEquals(Math.min(edge, 25 / 30.0), find(outcome, "AMBITION.tier").confidence());
        }
    }

    /** Every tier the labels use is accepted, in the spelling the schema gives. */
    @Test
    void everyTierIsAccepted() {
        for (String tier : List.of("toy", "utility", "product", "research")) {
            Fact fact = find(JudgedRules.apply(judgment("built", "{\"tier\": \"" + tier + "\", \"confidence\": 0.5, \"evidence\": [\"a\"]}", 3, "{}"), LIVE, List.of()), "AMBITION.tier");
            assertEquals(tier, fact.value());
        }
    }

    /** The measured facts are kept in order and the judged ones follow; the lists handed back cannot be changed. */
    @Test
    void theOutcomeIsAnImmutableCopy() {
        JudgedRules.Outcome outcome = JudgedRules.apply(judgment("own-work"), LIVE, MEASURED);
        assertEquals(MEASURED, outcome.facts().subList(0, MEASURED.size()));
        assertEquals(List.of("AMBITION.tier", "QUALITY.agent_review"), kinds(outcome.facts().subList(MEASURED.size(), outcome.facts().size())));
        try {
            outcome.facts().add(MEASURED.get(0));
            org.junit.jupiter.api.Assertions.fail("the facts are a copy");
        } catch (UnsupportedOperationException expected) {
            // the lists are immutable
        }
        try {
            outcome.notes().add("x");
            org.junit.jupiter.api.Assertions.fail("the notes are a copy");
        } catch (UnsupportedOperationException expected) {
            // the lists are immutable
        }
    }
}
