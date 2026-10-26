package evaluator.ingest;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * The folders a judge called the author's own, applied to an inventory (TODO.md 2.2j): a file belongs to its most specific
 * classified folder, and only a file whose folder is classed own code is counted by Size, Complexity and Maintainability's code
 * parts; a file in no classified folder keeps the kernel's own rule (it is counted). Own tests are not counted as code but are
 * kept for Maintainability's test-to-code ratio, and every other class is set aside with the judge's reason. The judge can only
 * narrow what the kernel counts: a file the kernel set aside as vendored or generated stays aside (E-025), whatever the judge says.
 */
// Decision: E-057
public final class OwnCode {
    /** The eight classes of a folder, in the judge's words (agent-runner/schema/judgment.schema.json). */
    public enum FolderClass {
        OWN_CODE("own code"), OWN_TESTS("own tests"), OWN_TOOLING("own tooling"), DOCS("docs"), GENERATED("generated"),
        VENDORED("vendored"), REFERENCE("reference"), FIXTURE_DATA("fixture data");

        /** The class as the judge writes it. */
        public final String title;

        FolderClass(String title) {
            this.title = title;
        }

        /** The class the judge's words name, or null for any other words. */
        public static FolderClass of(String title) {
            for (FolderClass c : values()) if (c.title.equals(title)) return c;
            return null;
        }
    }

    /** One classified folder: its path from the root (`.` is the root itself) and its class. */
    public record Folder(String path, FolderClass folderClass) {}

    /**
     * An inventory seen through a judgment. `counted` is what Size, Complexity and the code parts of Maintainability read: own code
     * and unclassified files, the rest set aside. `withTests` is `counted` plus the own tests, for the test-to-code ratio only.
     * `code` and `tests` are the paths the judge classed own code and own tests: the ratio takes these by class, not by name.
     */
    public record Split(Inventory counted, Inventory withTests, Set<String> code, Set<String> tests) {
        public Split {
            code = Set.copyOf(code);
            tests = Set.copyOf(tests);
        }
    }

    private OwnCode() {}

    /** The class of the most specific folder holding this path, or null when no classified folder holds it. */
    public static FolderClass classOf(String path, List<Folder> folders) {
        Folder best = mostSpecific(path, folders);
        return best == null ? null : best.folderClass();
    }

    /** The longest folder path that holds the path: `.` holds every path, `a/b` holds `a/b/c.py` and not `a/bc/d.py`. */
    static Folder mostSpecific(String path, List<Folder> folders) {
        Folder best = null;
        for (Folder f : folders) {
            boolean holds = f.path().equals(".") || path.startsWith(f.path() + "/");
            if (holds && (best == null || f.path().length() > best.path().length())) best = f;
        }
        return best;
    }

    /** The inventory through the folders: see the class comment. */
    public static Split split(Inventory inventory, List<Folder> folders) {
        List<Inventory.Source> counted = new ArrayList<>(), tests = new ArrayList<>();
        List<Inventory.Aside> aside = new ArrayList<>(inventory.setAside());
        Set<String> code = new TreeSet<>(), ownTests = new TreeSet<>();
        for (Inventory.Source file : inventory.source()) {
            Folder folder = mostSpecific(file.path(), folders);
            if (folder == null) {
                counted.add(file);
                continue;
            }
            switch (folder.folderClass()) {
                case OWN_CODE -> {
                    counted.add(file);
                    code.add(file.path());
                    continue;
                }
                case OWN_TESTS -> {
                    tests.add(file);
                    ownTests.add(file.path());
                }
                default -> { }
            }
            aside.add(new Inventory.Aside(file.path(), file.language(), "judged: " + folder.folderClass().title + " (folder " + folder.path() + ")"));
        }
        aside.sort(Comparator.comparing(Inventory.Aside::path));
        List<Inventory.Source> with = new ArrayList<>(counted);
        with.addAll(tests);
        with.sort(Comparator.comparing(Inventory.Source::path));
        return new Split(new Inventory(counted, aside), new Inventory(with, aside), code, ownTests);
    }
}
