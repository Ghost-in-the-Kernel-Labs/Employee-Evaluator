package evaluator.ingest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import evaluator.ingest.AgreementRecord.Fraction;
import evaluator.rubric.Calibration;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/**
 * An agreement record as the kernel reads it (TODO.md 2.2k, E-084): only the counts, and whether a field is live worked out from them as
 * `agent_runner.agreement.is_live` does, never from the record's own flag. The numbers are E-084's, on the 30 labels.
 */
class AgreementRecordTest {
    private static final Path ROOT = Path.of(System.getProperty("evaluator.root", "."));
    static final String HASH = "a".repeat(63) + "1";

    /** A record text: each field `matched/baseline` of 30 labels, every `live` flag written `flag`. */
    static String record(int ambition, int exact, int withinOne, int kind, boolean flag) {
        return """
                {"version": 1, "prompt": {"name": "t", "hash": "%s"}, "labels": 30, "fields": {
                  "ambition": {"matched": %d, "total": 30, "live": %b, "baseline": {"matched": 14}},
                  "quality_exact": {"matched": %d, "total": 30, "live": %b, "baseline": {"matched": 17}},
                  "quality_within_one": {"matched": %d, "total": 30, "live": %b, "baseline": {"matched": 27}},
                  "kind": {"matched": %d, "total": 30, "live": %b, "baseline": {"matched": 23}}}}
                """.formatted(HASH, ambition, flag, exact, flag, withinOne, flag, kind, flag);
    }

    static AgreementRecord parsed(int ambition, int exact, int withinOne, int kind) {
        return AgreementRecord.parse(record(ambition, exact, withinOne, kind, false), HASH);
    }

    /** E-084's counts, one under and at the mark for each field: Quality within one needs 29 (28 is 93.3 %, under 95 %), kind 27, Ambition 24 (the floor binds), Quality exactly 24. */
    @Test
    void eachFieldIsLiveAtItsMarkAndNotOneUnder() {
        assertFalse(parsed(23, 23, 28, 26).live("ambition"));
        assertTrue(parsed(24, 24, 29, 27).live("ambition"));
        assertFalse(parsed(24, 23, 28, 26).live("quality_exact"));
        assertTrue(parsed(24, 24, 29, 27).live("quality_exact"));
        assertFalse(parsed(24, 24, 28, 27).live("quality_within_one"));
        assertTrue(parsed(24, 24, 29, 27).live("quality_within_one"));
        assertFalse(parsed(24, 24, 29, 26).live("kind"));
        assertTrue(parsed(24, 24, 29, 27).live("kind"));
        assertTrue(parsed(30, 30, 30, 30).live("kind"));
        assertFalse(parsed(0, 0, 0, 0).live("quality_within_one"));
    }

    /** The two tests are separate: a field can clear one and miss the other. Here the floor is the binding one for Ambition (22 clears the margin, not 80 %). */
    @Test
    void theFloorAndTheMarginAreEachNeeded() {
        AgreementRecord r = parsed(22, 24, 29, 27);
        assertFalse(r.live("ambition"), "22 of 30 beats the baseline's margin (14 + 8) but is 73 %");
        assertTrue(r.live("ambition", new Fraction(1, 2), new Fraction(7, 10)), "a floor of 70 % lets it through, the margin held");
        assertFalse(r.live("ambition", new Fraction(1, 1), new Fraction(7, 10)), "a margin of the whole gap needs 30 of 30");
        assertTrue(parsed(30, 24, 29, 27).live("ambition", new Fraction(1, 1), new Fraction(0, 1)));
        assertTrue(r.live("ambition", new Fraction(0, 1), new Fraction(0, 1)), "no margin and no floor: anything is live");
    }

    /** A record's own flag is never read (E-084): a flag that lies in either direction changes nothing. */
    @Test
    void theStoredFlagIsNeverTrusted() {
        AgreementRecord lying = AgreementRecord.parse(record(22, 23, 28, 26, true), HASH);
        for (String field : AgreementRecord.FIELDS) assertFalse(lying.live(field), field);
        AgreementRecord modest = AgreementRecord.parse(record(24, 24, 29, 27, false), HASH);
        for (String field : AgreementRecord.FIELDS) assertTrue(modest.live(field), field);
    }

    @Test
    void whyNamesTheCountsAndIsNullWhenLive() {
        AgreementRecord r = parsed(24, 24, 28, 27);
        assertEquals("28 of 30 labels, 29 needed, the constant answer matches 27", r.why("quality_within_one"));
        assertNull(r.why("kind"));
        assertEquals("22 of 30 labels, 24 needed, the constant answer matches 14", parsed(22, 24, 29, 27).why("ambition"));
    }

    @Test
    void aCalibrationIsTheFigureOverAllTheLabels() {
        Calibration c = parsed(25, 24, 29, 27).calibration("ambition");
        assertEquals(HASH, c.promptHash());
        assertEquals(25 / 30.0, c.precision());
        assertEquals(30, c.labeled());
        assertThrows(IllegalArgumentException.class, () -> parsed(25, 24, 29, 27).calibration("verdict"));
        assertThrows(IllegalArgumentException.class, () -> parsed(25, 24, 29, 27).live("scores"));
    }

    /** A record that is not whole or not the prompt's is refused with a message, as read_record raises. */
    @Test
    void aRecordThatIsNotWholeIsRefused() {
        String good = record(24, 24, 29, 27, true);
        assertEquals(HASH, AgreementRecord.parse(good, HASH).promptHash());
        String other = "b".repeat(63) + "2";
        assertTrue(refusal(good, other).contains("not the record of prompt " + other));
        assertTrue(refusal(good.replace("\"version\": 1", "\"version\": 2"), HASH).contains("not version 1"));
        assertTrue(refusal(good, "xyz").contains("64 lowercase hex"));
        assertTrue(refusal(good.replace("\"ambition\": {", "\"ambitious\": {"), HASH).contains("fields.ambition must be a JSON object"));
        assertTrue(refusal(good.replace("\"matched\": 24, \"total\": 30, \"live\": true, \"baseline\": {\"matched\": 14}", "\"matched\": 2.5, \"total\": 30, \"baseline\": {\"matched\": 14}"), HASH).contains("fields.ambition: matched must be a whole number"));
        assertTrue(refusal(good.replace("\"matched\": 24, \"total\": 30, \"live\": true, \"baseline\": {\"matched\": 14}", "\"matched\": 31, \"total\": 30, \"baseline\": {\"matched\": 14}"), HASH).contains("fields.ambition: a field's counts"));
        assertTrue(refusal(good.replace("\"total\": 30, \"live\": true, \"baseline\": {\"matched\": 23}", "\"total\": 0, \"baseline\": {\"matched\": 0}"), HASH).contains("fields.kind"));
        assertTrue(refusal("[]", HASH).contains("the record must be a JSON object"));
        assertTrue(refusal("{", HASH).contains("not valid JSON"));
        assertTrue(refusal(good + " ".repeat(AgreementRecord.MAX_CHARS), HASH).contains("at most"));
        assertThrows(IllegalArgumentException.class, () -> new Fraction(2, 1));
        assertThrows(IllegalArgumentException.class, () -> new Fraction(1, 0));
        assertThrows(IllegalArgumentException.class, () -> new Fraction(-1, 2));
    }

    /** The edges of every size and count check: a count may be 0 and may be the total, a record may be as long as the cap and no longer. */
    @Test
    void theEdgesOfTheChecksAreExact() {
        for (long[] ok : new long[][] {{0, 1, 0}, {1, 1, 1}, {0, 1, 1}, {1, 1, 0}, {1_000_000, 1_000_000, 0}}) assertEquals(ok[0], new AgreementRecord.Field(ok[0], ok[1], ok[2]).matched());
        for (long[] bad : new long[][] {{0, 0, 0}, {-1, 1, 0}, {2, 1, 0}, {0, 1, 2}, {0, 1, -1}, {0, -1, 0}}) {
            assertThrows(IllegalArgumentException.class, () -> new AgreementRecord.Field(bad[0], bad[1], bad[2]), java.util.Arrays.toString(bad));
        }
        for (long[] ok : new long[][] {{0, 1}, {1, 1}, {0, 5}, {4, 5}}) assertEquals(ok[0], new Fraction(ok[0], ok[1]).numerator());
        for (long[] bad : new long[][] {{-1, 2}, {3, 2}, {1, 0}, {1, -1}, {0, 0}}) {
            assertThrows(IllegalArgumentException.class, () -> new Fraction(bad[0], bad[1]), java.util.Arrays.toString(bad));
        }
        String good = record(24, 24, 29, 27, true);
        assertEquals(HASH, AgreementRecord.parse(good + " ".repeat(AgreementRecord.MAX_CHARS - good.length()), HASH).promptHash(), "exactly the cap");
        String big = good.replace("\"matched\": 24, \"total\": 30, \"live\": true, \"baseline\": {\"matched\": 14}", "\"matched\": 1000000, \"total\": 1000000, \"baseline\": {\"matched\": 0}");
        assertEquals(1_000_000, AgreementRecord.parse(big, HASH).field("ambition").total());
        assertTrue(refusal(big.replace("\"matched\": 1000000, \"total\": 1000000", "\"matched\": 1000001, \"total\": 1000001"), HASH).contains("matched must be a whole number"));
        assertTrue(refusal(good.replace("\"matched\": 24, \"total\": 30, \"live\": true, \"baseline\": {\"matched\": 14}", "\"matched\": -1, \"total\": 30, \"baseline\": {\"matched\": 14}"), HASH).contains("matched must be a whole number"));
        assertEquals(0, AgreementRecord.parse(good.replace("\"matched\": 24, \"total\": 30, \"live\": true, \"baseline\": {\"matched\": 14}", "\"matched\": 0, \"total\": 30, \"baseline\": {\"matched\": 14}"), HASH).field("ambition").matched());
    }

    /** The fixtures' records, hand-written and synthetic: the all-live one and the one-under-each-mark one. */
    @Test
    void theFixtureRecordsAreRead() throws Exception {
        Path folder = ROOT.resolve("test/fixtures/judged/records");
        AgreementRecord live = read(folder, "a".repeat(63) + "1");
        for (String field : AgreementRecord.FIELDS) assertTrue(live.live(field), field);
        AgreementRecord none = read(folder, "b".repeat(63) + "2");
        for (String field : AgreementRecord.FIELDS) assertFalse(none.live(field), field);
        AgreementRecord lying = read(folder, "c".repeat(63) + "3");
        for (String field : AgreementRecord.FIELDS) assertFalse(lying.live(field), field + ": its flag says live, its counts say not");
        assertTrue(Files.readString(folder.resolve("c".repeat(63) + "3.json")).contains("\"live\": true"));
    }

    private static AgreementRecord read(Path folder, String hash) throws Exception {
        return AgreementRecord.parse(Files.readString(folder.resolve(hash + ".json")), hash);
    }

    private static String refusal(String text, String hash) {
        return assertThrows(IllegalArgumentException.class, () -> AgreementRecord.parse(text, hash), text).getMessage();
    }
}
