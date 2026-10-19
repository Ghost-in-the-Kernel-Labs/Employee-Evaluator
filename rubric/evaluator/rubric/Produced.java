package evaluator.rubric;

/** What made a card: the kernel's version, the date, and the rubric's hash. */
public record Produced(String kernelVersion, String date, String rubricHash) {}
