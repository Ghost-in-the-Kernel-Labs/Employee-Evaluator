package evaluator.ingest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import evaluator.ingest.OwnCode.Folder;
import evaluator.ingest.OwnCode.FolderClass;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** The judge's folders applied to an inventory (TODO.md 2.2j, E-057). */
class OwnCodeTest {
    private static Folder f(String path, FolderClass c) {
        return new Folder(path, c);
    }

    private static Inventory.Source s(String path, int lines) {
        return new Inventory.Source(path, "Python", lines);
    }

    @Test
    void theClassesAreTheJudgesWords() {
        assertEquals(8, FolderClass.values().length);
        for (FolderClass c : FolderClass.values()) assertEquals(c, FolderClass.of(c.title));
        assertEquals("own code", FolderClass.OWN_CODE.title);
        assertEquals("fixture data", FolderClass.FIXTURE_DATA.title);
        assertNull(FolderClass.of("Own Code"));
        assertNull(FolderClass.of("code"));
    }

    /** The longest folder holding the path wins, and a folder holds only whole path parts: `a/bc/x` is not in `a/b`. */
    @Test
    void aFileBelongsToItsMostSpecificFolder() {
        List<Folder> folders = List.of(f("a", FolderClass.DOCS), f("a/b", FolderClass.OWN_CODE), f("a/b/c", FolderClass.GENERATED));
        assertEquals(FolderClass.GENERATED, OwnCode.classOf("a/b/c/x.py", folders));
        assertEquals(FolderClass.OWN_CODE, OwnCode.classOf("a/b/x.py", folders));
        assertEquals(FolderClass.DOCS, OwnCode.classOf("a/bc/x.py", folders), "a/bc is not inside a/b");
        assertEquals(FolderClass.DOCS, OwnCode.classOf("a/x.py", folders));
        assertNull(OwnCode.classOf("other/x.py", folders), "no classified folder holds it");
        assertNull(OwnCode.classOf("a", folders), "a folder's own name is not a file inside it");
        assertEquals(FolderClass.OWN_CODE, OwnCode.classOf("a/b/x.py", List.of(f("a/b", FolderClass.OWN_CODE), f("a", FolderClass.DOCS))), "the order listed does not matter");
    }

    /** The root `.` holds everything, but loses to any deeper folder, whatever order they are listed in. */
    @Test
    void theRootHoldsEverythingAndLosesToAnyDeeperFolder() {
        List<Folder> folders = List.of(f(".", FolderClass.DOCS), f("src", FolderClass.OWN_CODE));
        assertEquals(FolderClass.DOCS, OwnCode.classOf("README.py", folders));
        assertEquals(FolderClass.OWN_CODE, OwnCode.classOf("src/a.py", folders));
        assertEquals(FolderClass.OWN_CODE, OwnCode.classOf("src/a.py", List.of(f("src", FolderClass.OWN_CODE), f(".", FolderClass.DOCS))));
    }

    /**
     * Worked by hand: src/ own code, src/gen generated, atests/ own tests, docs/ docs, docs/tools own code, scripts/ tooling;
     * extra/ is in no folder. Counted: src/a (3), docs/tools/t (2), extra/h (4), extra/test_h (1) = 4 files, 10 lines; the atests/t file is kept
     * for the ratio; src/gen/g, docs/d, scripts/r and atests/t are set aside with the judge's class and folder.
     */
    @Test
    void onlyOwnCodeAndWhatNoFolderHoldsIsCounted() {
        Inventory inventory = new Inventory(List.of(s("docs/d.py", 1), s("docs/tools/t.py", 2), s("extra/h.py", 4), s("extra/test_h.py", 1),
                s("scripts/r.py", 5), s("src/a.py", 3), s("src/gen/g.py", 6), s("atests/t.py", 7)),
                List.of(new Inventory.Aside("src/node_modules/x.py", "Python", "vendored: folder node_modules/")));
        List<Folder> folders = List.of(f("src", FolderClass.OWN_CODE), f("src/gen", FolderClass.GENERATED), f("atests", FolderClass.OWN_TESTS),
                f("docs", FolderClass.DOCS), f("docs/tools", FolderClass.OWN_CODE), f("scripts", FolderClass.OWN_TOOLING));
        OwnCode.Split split = OwnCode.split(inventory, folders);
        assertEquals(List.of("docs/tools/t.py", "extra/h.py", "extra/test_h.py", "src/a.py"), split.counted().source().stream().map(Inventory.Source::path).toList());
        assertEquals(List.of("atests/t.py", "docs/tools/t.py", "extra/h.py", "extra/test_h.py", "src/a.py"),
                split.withTests().source().stream().map(Inventory.Source::path).toList());
        assertEquals(Set.of("docs/tools/t.py", "src/a.py"), split.code());
        assertEquals(Set.of("atests/t.py"), split.tests());
        assertEquals(List.of("atests/t.py|judged: own tests (folder atests)", "docs/d.py|judged: docs (folder docs)", "scripts/r.py|judged: own tooling (folder scripts)",
                        "src/gen/g.py|judged: generated (folder src/gen)", "src/node_modules/x.py|vendored: folder node_modules/"),
                split.counted().setAside().stream().map(a -> a.path() + "|" + a.reason()).toList(), "in path order, the kernel's own reason kept");
        assertEquals(split.counted().setAside(), split.withTests().setAside());
        assertEquals(10, split.counted().source().stream().mapToInt(Inventory.Source::lines).sum(), "2 + 4 + 1 + 3 lines");
    }

    /** The judge can narrow, never widen: a file the kernel set aside stays aside whatever the judge calls its folder. */
    @Test
    void aFileTheKernelSetAsideStaysAside() {
        Inventory inventory = new Inventory(List.of(s("src/a.py", 1)), List.of(new Inventory.Aside("src/vendor/v.py", "Python", "vendored: folder vendor/")));
        OwnCode.Split split = OwnCode.split(inventory, List.of(f("src", FolderClass.OWN_CODE), f("src/vendor", FolderClass.OWN_CODE)));
        assertEquals(List.of("src/a.py"), split.counted().source().stream().map(Inventory.Source::path).toList());
        assertEquals(List.of("src/vendor/v.py"), split.counted().setAside().stream().map(Inventory.Aside::path).toList());
    }

    @Test
    void noFoldersMeansTheInventoryAsItIs() {
        Inventory inventory = new Inventory(List.of(s("a.py", 1), s("t/b.py", 2)), List.of());
        OwnCode.Split split = OwnCode.split(inventory, List.of());
        assertEquals(inventory, split.counted());
        assertEquals(inventory, split.withTests());
        assertEquals(Set.of(), split.code());
        assertEquals(Set.of(), split.tests());
    }
}
