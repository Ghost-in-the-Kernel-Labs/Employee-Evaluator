package evaluator.rubric;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * One hand label of the calibration set (TODO.md 2.1): Ghost's reading of one repository, keyed by an opaque id.
 * The id-to-repository map lives outside this repository (E-014), so no field may name a repository, an owner,
 * a URL or a person; the closed fields are enums and the one free field, the rationale, is checked for anything name-like.
 */
// Decision: E-079
public record Label(String id, Domain domain, Kind kind, Ambition ambition, int quality, String rationale) {
    /** The id's shape: `c` and three digits, opaque by construction. */
    static final Pattern ID = Pattern.compile("c[0-9]{3}");
    /** The longest a rationale may be. */
    static final int MAX_RATIONALE = 280;
    /** The Quality scale a hand label uses (docs/DESIGN.md step 2.1): 1 to 5. */
    public static final int QUALITY_MIN = 1, QUALITY_MAX = 5;

    private static final Pattern URL_LIKE = Pattern.compile(
            "(?i)[a-z][a-z0-9+.-]*://|\\bwww\\.|@|\\b(github|gitlab|bitbucket)\\b|\\b[a-z0-9-]+\\.(com|io|org|net|dev|app|ai|co|me|sh|xyz)\\b|\\b[\\w.-]+/[\\w.-]+\\b");
    /** Two capitalised words in a row read as a person's or a project's name. */
    private static final Pattern NAME_LIKE = Pattern.compile("\\b[A-Z][a-z]+ [A-Z][a-z]+\\b");

    /** What the repository is about; the set must span at least five of these. */
    public enum Domain {
        WEB_APP, CLI_TOOL, LIBRARY, DATA_PIPELINE, GAME, MOBILE_APP, INFRASTRUCTURE, MACHINE_LEARNING, HARDWARE, CRYPTO_DEFI, DEVELOPER_TOOLING, AGENT;
    }

    /** For the admission gate (E-056): the work is the applicant's own, a clone of another's, or generated wholesale by a tool. */
    public enum Kind {
        OWN_WORK, CLONE, GENERATED;
    }

    /** How hard the stated goal was: the rubric's scale (docs/SCORE-CARD.md). */
    public enum Ambition {
        TOY, UTILITY, PRODUCT, RESEARCH;
    }

    public Label {
        if (id == null || !ID.matcher(id).matches()) throw new IllegalArgumentException("a label's id is opaque, c and three digits, not " + id);
        if (domain == null || kind == null || ambition == null) throw new IllegalArgumentException("a label has a domain, a kind and an ambition");
        if (quality < QUALITY_MIN || quality > QUALITY_MAX) throw new IllegalArgumentException("a label's quality is " + QUALITY_MIN + "–" + QUALITY_MAX + ", not " + quality);
        if (rationale == null) throw new IllegalArgumentException("a label's rationale is text, empty at the least");
        if (rationale.length() > MAX_RATIONALE) throw new IllegalArgumentException("a rationale is " + MAX_RATIONALE + " characters at most");
        if (rationale.contains("\n")) throw new IllegalArgumentException("a rationale is one line");
        if (URL_LIKE.matcher(rationale).find()) throw new IllegalArgumentException("a rationale holds nothing URL-like, no address, owner/repo or host (E-014)");
        if (NAME_LIKE.matcher(rationale).find()) throw new IllegalArgumentException("a rationale holds nothing name-like, no two capitalised words in a row (E-014)");
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
