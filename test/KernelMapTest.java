import java.util.List;
import java.util.Map;

/**
 * Holds docs/KERNEL-MAP.md (docs/AGENT.md): every function in a module has a row, and a
 * function removed or renamed has a later row closing it (`removed:` or `superseded-by:`), which
 * `gradle kernelMap` writes itself (E-062).
 */
final class KernelMapTest {
    public static void main(String[] args) {
        run().exit();
    }

    static Check run() {
        Map<String, String> one = Map.of("ingest/A.java", "class A { int f() { return 1; } }");
        String held = KernelMap.write(HEADER, one, "2026-10-04");
        return new Check("kernel map")
                .repository(KernelMap.check(Check.read(KernelMap.PATH), Repo.moduleSources()))
                .rejects("a function the map does not hold", () -> KernelMap.check(HEADER, one))
                .rejects("a function gone with no row closing it", () -> KernelMap.check(held, Map.of()))
                .accepts("a function closed by a later row", () -> KernelMap.check(held + "| ingest | `ingest/A.java:A.f` |  | 2026-10-05 | removed: abc1234 |\n", Map.of()))
                .rejects("a successor the map does not hold", () -> KernelMap.check(held + "| ingest | `ingest/A.java:A.f` |  | 2026-10-05 | superseded-by: `ingest/A.java:A.g` |\n", Map.of()))
                .accepts("the map the generator just wrote", () -> KernelMap.check(held, one))
                .accepts("a decision cited under a Javadoc", () -> KernelMap.write(HEADER, CITED, "2026-10-04").contains("| E-007 |") ? List.of() : List.of("no E-007"))
                .accepts("a write closes a function gone as removed", () -> KernelMap.write(held, Map.of(), "2026-10-23").endsWith("| ingest | `ingest/A.java:A.f` |  | 2026-10-23 | removed: 2026-10-23 |\n") ? KernelMap.check(KernelMap.write(held, Map.of(), "2026-10-23"), Map.of()) : List.of("not closed"))
                .accepts("a write closes a moved function as superseded by its new place", () -> moved(held))
                .accepts("a nested class moved to its own file is superseded, not removed", () -> unnested())
                .accepts("a second write, which adds nothing", () -> held.equals(KernelMap.write(held, one, "2026-10-06")) ? List.of() : List.of("changed"));
    }

    private static final Map<String, String> CITED = Map.of("ingest/B.java", """
            class B {
                /** What it is for. */
                // Decision: E-007
                int g() { return 2; }
            }
            """);

    private static final String HEADER = """
            | Module | Function | Stands on | First seen | Superseded |
            | --- | --- | --- | --- | --- |
            """;

    /** A.f moved to B.java: the write closes the old key as superseded by the new one, and the map then checks. */
    private static List<String> moved(String held) {
        Map<String, String> after = Map.of("ingest/B.java", "class A { int f() { return 1; } }");
        String map = KernelMap.write(held, after, "2026-10-23");
        if (!map.contains("| ingest | `ingest/A.java:A.f` |  | 2026-10-23 | superseded-by: `ingest/B.java:A.f` |\n")) return List.of("not superseded: " + map);
        return KernelMap.check(map, after);
    }

    /** Outer.Inner.f moved to Inner.java as Inner.f: superseded by its new key. */
    private static List<String> unnested() {
        String before = KernelMap.write(HEADER, Map.of("ingest/Outer.java", "class Outer { static class Inner { int f() { return 1; } } }"), "2026-10-04");
        Map<String, String> after = Map.of("ingest/Inner.java", "class Inner { int f() { return 1; } }", "ingest/Outer.java", "class Outer { }");
        String map = KernelMap.write(before, after, "2026-10-23");
        if (!map.contains("| ingest | `ingest/Outer.java:Outer.Inner.f` |  | 2026-10-23 | superseded-by: `ingest/Inner.java:Inner.f` |")) return List.of("not superseded: " + map);
        return KernelMap.check(map, after);
    }
}
