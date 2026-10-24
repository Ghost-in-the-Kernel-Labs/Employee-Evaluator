package evaluator.rubric;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * One hand label of the calibration set (TODO.md 2.1): Ghost's reading of one repository, keyed by an opaque id.
 * The id-to-repository map lives outside this repository (E-014); every field is an id, an enum or a number, so
 * no field can hold a repository, an owner, a URL or a person.
 */
// Decision: E-080
public record Label(String id, Kind kind, Ambition ambition, int quality) {
    /** The id's shape: `c` and three digits, opaque by construction. */
    static final Pattern ID = Pattern.compile("c[0-9]{3}");
    /** The Quality scale a hand label uses (docs/DESIGN.md step 2.1): 1 to 5. */
    public static final int QUALITY_MIN = 1, QUALITY_MAX = 5;

    /** For the admission gate (E-056): the work is the applicant's own, a clone of another's, or generated wholesale by a tool. */
    public enum Kind {
        OWN_WORK, CLONE, GENERATED;
    }

    /** How hard the stated goal was: the rubric's scale (docs/SCORE-CARD.md). */
    public enum Ambition {
        TOY, UTILITY, PRODUCT, RESEARCH;
    }

    /** Refuses a label whose id is not opaque, whose kind or ambition is missing, or whose quality is out of range (E-080). */
    public Label {
        if (id == null || !ID.matcher(id).matches()) throw new IllegalArgumentException("a label's id is opaque, c and three digits, not " + id);
        if (kind == null || ambition == null) throw new IllegalArgumentException("a label has a kind and an ambition");
        if (quality < QUALITY_MIN || quality > QUALITY_MAX) throw new IllegalArgumentException("a label's quality is " + QUALITY_MIN + "–" + QUALITY_MAX + ", not " + quality);
    }

    /** An enum value as the fixture spells it: lowercase, hyphenated. */
    static String wire(Enum<?> value) {
        return value.name().toLowerCase(Locale.ROOT).replace('_', '-');
    }

    /** The values of an enum as the fixture spells them, for the error that names the closed list. */
    static List<String> wires(Enum<?>[] values) {
        return java.util.Arrays.stream(values).map(Label::wire).toList();
    }
}
