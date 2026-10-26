package evaluator.ingest;

import evaluator.rubric.Calibration;
import evaluator.rubric.Json;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * The agreement of one judging prompt with the calibration set, as `agent-runner/agent_runner/agreement.py` recorded it under the prompt's hash
 * (`agent-runner/records/<hash>.json`, TODO.md 2.2i, read here for 2.2k). The file is data (E-013); nothing is run and no model is called (E-003).
 * Only the counts are read: for each of the four fields the labels matched, the labels there were, and the constant-answer baseline's matches.
 * Whether a field is live is worked out here from those counts, the record's own `live` flag never read (E-084), as `agreement.is_live` does,
 * so a changed margin or floor takes effect on an old record and a hand-edited flag changes nothing.
 */
// Decision: E-084
public record AgreementRecord(String promptHash, Map<String, Field> fields) {
    /** The four judged fields a record holds, as `agreement.FIELDS` names them. */
    public static final List<String> FIELDS = List.of("ambition", "quality_exact", "quality_within_one", "kind");
    /** The record's version, `agreement.VERSION`. */
    static final int VERSION = 1;
    /** The raw agreement a live field needs, `agreement.FLOOR`: four fifths. */
    public static final Fraction FLOOR = new Fraction(4, 5);
    /** The share of the gap to 100 % a live field must close over its baseline, `agreement.MARGIN`: one half. */
    // Unsure: half the gap is the agent's proposal for Ghost to confirm (E-084), as in agreement.py; one constant here, a parameter of live().
    public static final Fraction MARGIN = new Fraction(1, 2);
    /** The biggest record file read, in characters; a record of 30 ids is some 20 thousand. */
    public static final int MAX_CHARS = 1_000_000;
    private static final Pattern HASH = Pattern.compile("[0-9a-f]{64}");

    /** A fraction of whole numbers, never a float, so 24 of 30 is exactly four fifths. */
    public record Fraction(long numerator, long denominator) {
        public Fraction {
            if (numerator < 0 || denominator <= 0 || numerator > denominator) throw new IllegalArgumentException("a fraction is between 0 and 1, not " + numerator + "/" + denominator);
        }
    }

    /** One field's counts: the labels matched, all the labels, and the matches of the constant answer. */
    public record Field(long matched, long total, long baseline) {
        public Field {
            if (total <= 0 || matched < 0 || matched > total || baseline < 0 || baseline > total) {
                throw new IllegalArgumentException("a field's counts are 0 <= matched <= total and 0 <= baseline <= total, with a total above 0");
            }
        }

        /** The least count of matches that is live: `agreement.needed`, the larger of the floor and the margin's mark, rounded up. */
        long needed(Fraction margin, Fraction floor) {
            long byMargin = baseline + Math.ceilDiv(margin.numerator() * (total - baseline), margin.denominator());
            long byFloor = Math.ceilDiv(floor.numerator() * total, floor.denominator());
            return Math.max(byFloor, byMargin);
        }
    }

    public AgreementRecord {
        fields = Map.copyOf(fields);
    }

    /** The record `text` holds, which must be the record of `promptHash`; anything else is an IllegalArgumentException saying what. */
    public static AgreementRecord parse(String text, String promptHash) {
        if (promptHash == null || !HASH.matcher(promptHash).matches()) throw new IllegalArgumentException("the prompt hash is 64 lowercase hex digits");
        if (text.length() > MAX_CHARS) throw new IllegalArgumentException("a record is at most " + MAX_CHARS + " characters, this one has " + text.length());
        Object parsed;
        try {
            parsed = Json.parse(text);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("not valid JSON (" + e.getMessage() + ")");
        }
        Map<?, ?> top = object(parsed, "the record");
        if (!(top.get("version") instanceof Double version) || version != VERSION) throw new IllegalArgumentException("the record is not version " + VERSION);
        if (!(object(top.get("prompt"), "prompt").get("hash") instanceof String hash) || !hash.equals(promptHash)) {
            throw new IllegalArgumentException("the record is not the record of prompt " + promptHash);
        }
        Map<?, ?> fields = object(top.get("fields"), "fields");
        Map<String, Field> counts = new java.util.LinkedHashMap<>();
        for (String field : FIELDS) {
            Map<?, ?> entry = object(fields.get(field), "fields." + field);
            Map<?, ?> baseline = object(entry.get("baseline"), "fields." + field + ".baseline");
            try {
                counts.put(field, new Field(whole(entry.get("matched"), "matched"), whole(entry.get("total"), "total"), whole(baseline.get("matched"), "baseline.matched")));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("fields." + field + ": " + e.getMessage());
            }
        }
        return new AgreementRecord(promptHash, counts);
    }

    /** Whether the prompt is live for `field` under the rule as it now stands: the raw agreement at least the floor and the figure at least the margin's mark over the baseline (E-084). */
    public boolean live(String field) {
        return live(field, MARGIN, FLOOR);
    }

    /** Whether `field` is live under this margin and floor; the counts are the record's, its own flag is never read. */
    public boolean live(String field, Fraction margin, Fraction floor) {
        Field f = field(field);
        boolean underFloor = f.matched() * floor.denominator() < floor.numerator() * f.total();
        boolean underMargin = f.matched() < f.baseline() + Math.ceilDiv(margin.numerator() * (f.total() - f.baseline()), margin.denominator());
        return !underFloor && !underMargin;
    }

    /** Why `field` is not live, in words with the counts, or null when it is. */
    public String why(String field) {
        if (live(field)) return null;
        Field f = field(field);
        return f.matched() + " of " + f.total() + " labels, " + f.needed(MARGIN, FLOOR) + " needed, the constant answer matches " + f.baseline();
    }

    /** The precision a fact of `field` may claim: the labels matched over all the labels, with how many there were (SC-05). */
    public Calibration calibration(String field) {
        Field f = field(field);
        return new Calibration(promptHash, (double) f.matched() / f.total(), (int) f.total());
    }

    /** The field's counts; a name that is not one of the four is refused. */
    Field field(String field) {
        Field f = fields.get(field);
        if (f == null) throw new IllegalArgumentException("field must be one of " + String.join(", ", FIELDS));
        return f;
    }

    private static Map<?, ?> object(Object value, String where) {
        if (!(value instanceof Map<?, ?> map)) throw new IllegalArgumentException(where + " must be a JSON object");
        return map;
    }

    private static long whole(Object value, String where) {
        if (!(value instanceof Double d) || d != Math.rint(d) || d < 0 || d > 1_000_000) throw new IllegalArgumentException(where + " must be a whole number");
        return d.longValue();
    }
}
