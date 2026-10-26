package evaluator.analyze;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** A report narrowed to some files (TODO.md 2.2j). */
class ComplexityReportTest {
    @Test
    void aReportKeepsOnlyTheNamedFilesInEveryPart() {
        Measure m = new Measure("f", 1, 1, 0, 1, "");
        Import i = new Import("os", List.of(), 1, List.of());
        Complexity.Report whole = new Complexity.Report(Map.of("a.py", List.of(m), "b.py", List.of(m), "c.py", List.of()),
                Map.of("a.py", List.of(i), "b.py", List.of(i)), Map.of("d.py", "bad", "a.py", "worse"));
        Complexity.Report kept = whole.restrictedTo(Set.of("a.py", "c.py", "zzz.py"));
        assertEquals(List.of("a.py", "c.py"), List.copyOf(kept.functions().keySet()));
        assertEquals(List.of("a.py"), List.copyOf(kept.imports().keySet()));
        assertEquals(Map.of("a.py", "worse"), kept.unread());
        assertEquals(3, whole.functions().size(), "the whole is not changed");
        assertEquals(Map.of(), whole.restrictedTo(Set.of()).functions());
    }
}
