package evaluator.ingest;

import evaluator.rubric.Evidence;
import evaluator.rubric.Fact;
import evaluator.rubric.Label;
import evaluator.rubric.Metric;
import evaluator.rubric.Subject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * The card's rules for the judged kind (TODO.md 2.2k, E-056, E-057, E-082, E-086): what a judge's answer is allowed to change about a card, given the
 * recorded agreement of its prompt (`AgreementRecord`). The judge classifies and judges; the kernel applies these rules, and a judged value counts
 * only where the prompt's record says that field is live (E-084). No model is called (E-003): the answer and the record are files read as data.
 *
 * <ul>
 * <li>A field not live gives no fact and no effect, and the outcome says so in a note: the kind decides nothing, an Ambition or a Quality is not a fact (SC-05).
 * <li>Kind `generated`: declined, no card (E-056). `plan only`: Size, Complexity and Maintainability are withheld and Ambition is unverified (E-057).
 * <li>Kind `clone`: scored, with a note; E-082 says a clone call alone is not a decline, and no decision yet says more (E-086, an open question for Ghost).
 * <li>`built`, `mixed`, `own-work`: scored as measured, the kind named in a note.
 * <li>Facts: `AMBITION.tier` (live Ambition) and `QUALITY.agent_review` (the whole 1 to 5, gated by Quality within one), each made with `Fact.judged`,
 *     its confidence the judge's own capped at the record's agreement for that field (SC-05).
 * </ul>
 */
// Decision: E-086
public final class JudgedRules {
    /** What the rules made of an answer: whether the submission is declined, the facts the card is made from, and what the card should say. */
    public record Outcome(boolean declined, List<Fact> facts, List<String> notes) {
        public Outcome {
            facts = List.copyOf(facts);
            notes = List.copyOf(notes);
        }
    }

    /** The metrics a plan carries nothing for, because there is no running code to count (E-057). */
    static final Set<Metric> WITHHELD_FOR_A_PLAN = Set.of(Metric.SIZE, Metric.COMPLEXITY, Metric.MAINTAINABILITY);
    static final String AMBITION = "AMBITION.tier", QUALITY = "QUALITY.agent_review";

    private JudgedRules() {}

    /** The outcome for `measured` facts under `judgment` and the record of its prompt; `record` is not null (no record, no rule: the caller leaves the facts as they are, SC-05). */
    public static Outcome apply(Judgment judgment, AgreementRecord record, List<Fact> measured) {
        List<String> notes = new ArrayList<>();
        List<Fact> facts = new ArrayList<>(measured);
        boolean plan = false;
        if (record.live("kind")) {
            String agreement = agreement(record, "kind");
            switch (judgment.kind()) {
                case "generated" -> {
                    return new Outcome(true, List.of(), List.of("kind: generated, declined (" + agreement + ")"));
                }
                case "plan only" -> {
                    plan = true;
                    facts.removeIf(f -> WITHHELD_FOR_A_PLAN.contains(f.metric()));
                    notes.add("kind: plan only (" + agreement + "): Size, Complexity and Maintainability are withheld");
                }
                case "clone" -> notes.add("kind: clone (" + agreement + "): scored, a clone call alone is no decline (E-082)");
                default -> notes.add("kind: " + judgment.kind() + " (" + agreement + ")");
            }
        } else {
            notes.add("kind: unverified, not acted on (" + record.why("kind") + ")");
        }
        if (plan) notes.add("ambition: unverified (a plan only repository, E-057)");
        else judgedFact(AMBITION, "ambition", "ambition", judgment, record, judgment.ambition(), tier(judgment), notes, facts);
        judgedFact(QUALITY, "quality", "quality_within_one", judgment, record, judgment.qualityReview(), judgment.quality(), notes, facts);
        return new Outcome(false, facts, notes);
    }

    /**
     * One judged fact, or the note saying why there is none. `member` holds the judge's own `confidence` and `evidence`; `value` is the judged value (null when
     * the answer does not hold it). The fact's confidence is the judge's, never above the record's agreement for `field` (SC-05).
     */
    // Decision: E-017
    private static void judgedFact(String kind, String name, String field, Judgment judgment, AgreementRecord record,
            java.util.Map<String, Object> member, Object value, List<String> notes, List<Fact> facts) {
        if (!record.live(field)) {
            notes.add(name + ": unverified (" + record.why(field) + ")");
            return;
        }
        Double claimed = member.get("confidence") instanceof Double c && c >= 0 && c <= 1 ? c : null;
        List<String> sources = member.get("evidence") instanceof List<?> list && !list.isEmpty() && list.stream().allMatch(i -> i instanceof String s && !s.isBlank())
                ? list.stream().map(String.class::cast).toList() : List.of();
        if (value == null || claimed == null || sources.isEmpty()) {
            notes.add(name + ": unverified (the judge's answer does not hold a whole " + name + " with a confidence between 0 and 1 and its evidence)");
            return;
        }
        Evidence evidence = new Evidence("judge prompt " + judgment.promptHash() + " (" + String.join(", ", judgment.models()) + ")", sources);
        Fact fact = Fact.judged(kind, Subject.repository(), value, evidence, claimed, record.calibration(field));
        facts.add(fact);
        notes.add(name + ": " + value + ", confidence " + shown(fact.confidence()) + " (the judge claimed " + shown(claimed) + ", " + agreement(record, field) + ")");
    }

    /** The judge's Ambition tier when it is one of the four the labels use, else null. */
    private static String tier(Judgment judgment) {
        Object tier = judgment.ambition().get("tier");
        return Arrays.stream(Label.Ambition.values()).map(a -> a.name().toLowerCase(Locale.ROOT)).filter(a -> a.equals(tier)).findFirst().orElse(null);
    }

    /** "agreement 27 of 30", the record's matches for the field. */
    private static String agreement(AgreementRecord record, String field) {
        return "agreement " + record.field(field).matched() + " of " + record.field(field).total();
    }

    private static String shown(double confidence) {
        return String.valueOf(Math.round(confidence * 1000) / 1000.0);
    }
}
