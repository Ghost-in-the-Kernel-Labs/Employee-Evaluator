package evaluator.analyze;

/**
 * One function's complexity: its name within its file (`Outer.Inner.method`, `#2` for a second of one
 * name), the line it starts on, its cyclomatic complexity (decision points plus one) and its
 * cognitive complexity (SonarSource's: breaks in linear flow, weighted by nesting).
 */
public record Measure(String function, int line, int cyclomatic, int cognitive) {}
