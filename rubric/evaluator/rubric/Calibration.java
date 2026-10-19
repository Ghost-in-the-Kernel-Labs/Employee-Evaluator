package evaluator.rubric;

/** A judging prompt's recorded precision on the labeled calibration set, by the prompt's hash. */
public record Calibration(String promptHash, double precision, int labeled) {
    public Calibration {
        if (promptHash == null || promptHash.isBlank()) throw new IllegalArgumentException("a calibration names its prompt's hash");
        if (precision < 0 || precision > 1) throw new IllegalArgumentException("a precision is 0–1, not " + precision);
        if (labeled <= 0) throw new IllegalArgumentException("a calibration was measured on labeled repositories");
    }
}
