package evaluator.analyze;

/**
 * One function's complexity: its name within its file (`Outer.Inner.method`, `#2` for a second of one
 * name), the line it starts on, its cyclomatic complexity (decision points plus one) and its
 * cognitive complexity (SonarSource's: breaks in linear flow, weighted by nesting); and the line it
 * ends on and its source text, which duplication reads (E-036).
 */
public record Measure(String function, int line, int cyclomatic, int cognitive, int endLine, String text) {
    /** Whether this function lies inside another (a closure, a method of a class inside a function). */
    public boolean within(Measure outer) {
        return outer != this && outer.line() <= line && endLine <= outer.endLine() && (outer.line() < line || endLine < outer.endLine());
    }
}
