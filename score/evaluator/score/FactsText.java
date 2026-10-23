package evaluator.score;

import evaluator.rubric.Fact;
import evaluator.rubric.Subject;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * The values behind a card, as plain text (`evaluator score --facts`): one line per value, `name value`, sorted by name. The
 * repository-wide facts as measured, then the inputs a counted metric is scored from (Size's line count is a fact already;
 * Complexity's cognitive total; Maintainability's parts and weighted score), each computed by the code that scores with it.
 * It makes no fact and scores nothing.
 */
// Decision: E-076
public final class FactsText {
    private FactsText() {}

    /** The lines for these facts, each ending in a newline; empty for no facts. */
    public static String render(List<Fact> facts) {
        List<String> lines = new ArrayList<>();
        for (Fact f : facts) if (f.subject().kind() == Subject.Kind.REPOSITORY) lines.add(f.kind() + " " + text(f.value()));
        List<Fact> functions = Formulas.cognitiveFacts(facts);
        if (!functions.isEmpty()) {
            lines.add("COMPLEXITY.cognitive_total " + text(Formulas.cognitiveTotal(functions)));
            lines.add("COMPLEXITY.functions " + functions.size());
        }
        List<Fact> files = facts.stream().filter(f -> f.kind().equals("MAINTAINABILITY.fan_out")).toList();
        if (!files.isEmpty()) lines.add("MAINTAINABILITY.files " + files.size());
        for (Map.Entry<String, Double> part : Maintainability.parts(facts, new ArrayList<>()).entrySet()) {
            lines.add("MAINTAINABILITY.part." + part.getKey() + " " + text(part.getValue()));
        }
        Double weighted = Maintainability.weighted(facts, new ArrayList<>());
        if (weighted != null) lines.add("MAINTAINABILITY.weighted " + text(weighted));
        Collections.sort(lines);
        return lines.isEmpty() ? "" : String.join("\n", lines) + "\n";
    }

    /** A value as plain text: a whole number without a decimal point, a fraction in full and never in exponent form. */
    static String text(Object value) {
        if (value instanceof Double d && !d.isInfinite() && !d.isNaN()) {
            return new BigDecimal(Double.toString(d)).stripTrailingZeros().toPlainString();
        }
        return String.valueOf(value);
    }
}
