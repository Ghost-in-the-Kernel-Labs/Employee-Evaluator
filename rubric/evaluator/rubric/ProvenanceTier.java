package evaluator.rubric;

/** The provenance tier the card shows beside the score (E-012: a score, never a gate). */
// Decision: E-012
public enum ProvenanceTier {
    VERIFIED("Verified"),
    PARTIAL("Partial"),
    UNVERIFIED("Unverified");

    /** The tier's name as the card writes it. */
    public final String title;

    ProvenanceTier(String title) {
        this.title = title;
    }

    /** Verified at 80 and over, Partial from 50 to 79, Unverified under 50. */
    public static ProvenanceTier of(int provenance) {
        if (provenance >= 80) return VERIFIED;
        return provenance >= 50 ? PARTIAL : UNVERIFIED;
    }
}
