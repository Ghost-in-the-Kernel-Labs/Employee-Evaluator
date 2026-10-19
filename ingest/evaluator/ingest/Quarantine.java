package evaluator.ingest;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Takes out of a submission's checkout everything an agent or an editor would run on opening it: the
 * agent and editor config folders and files at any depth, and git's hooks. Run on our copy of the
 * submission before any tool or agent touches it (E-033). No link is followed: a config that is a
 * link is removed as a link, and what it points to is left alone.
 */
// Decision: E-033
public final class Quarantine {
    /** Folders an agent or editor reads on opening a checkout, and may run from. */
    static final Set<String> FOLDERS = Set.of(".claude", ".vscode", ".cursor", ".gemini");
    /** Files an agent reads on opening a checkout, and may start servers from. */
    static final Set<String> FILES = Set.of(".mcp.json");

    private Quarantine() {}

    /** Removes them from the checkout at root; returns what was removed, by path from root, in order. */
    public static List<String> strip(Path root) {
        Path base = root.toAbsolutePath().normalize();
        List<Path> doomed = new ArrayList<>();
        try {
            Files.walkFileTree(base, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    String name = dir.getFileName() == null ? "" : dir.getFileName().toString();
                    if (FOLDERS.contains(name) || dir.endsWith(Path.of(".git", "hooks"))) {
                        doomed.add(dir);
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    String name = file.getFileName().toString();
                    if (FILES.contains(name) || (attrs.isSymbolicLink() && FOLDERS.contains(name))) doomed.add(file);
                    return FileVisitResult.CONTINUE;
                }
            });
            List<String> removed = new ArrayList<>();
            for (Path path : doomed) {
                delete(path);
                removed.add(base.relativize(path).toString().replace('\\', '/'));
            }
            removed.sort(null);
            return removed;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** A file, a link, or a folder and everything in it, never following a link out of it. */
    private static void delete(Path path) throws IOException {
        if (Files.isSymbolicLink(path) || !Files.isDirectory(path)) {
            Files.delete(path);
            return;
        }
        Files.walkFileTree(path, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException failed) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }
}
