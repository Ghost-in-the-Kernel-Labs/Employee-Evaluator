package evaluator.ingest;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Reads a checked-out repository into an Inventory. The repository is hostile input (E-013): no link
 * is followed, `.git` is not read, a binary file is not source, and a file's bytes are only counted,
 * never decoded as anything but single bytes.
 */
// Decision: E-013
public final class Ingest {
    private Ingest() {}

    /** The inventory of the repository at root: every source file counted or set aside, in path order. */
    public static Inventory read(Path root) {
        Path base = root.toAbsolutePath().normalize();
        SetAside rules = new SetAside(gitattributes(base));
        List<Inventory.Source> source = new ArrayList<>();
        List<Inventory.Aside> aside = new ArrayList<>();
        for (Path file : files(base)) {
            String path = base.relativize(file).toString().replace('\\', '/');
            String language = Languages.of(path);
            if (language == null) continue;
            byte[] bytes = bytes(file);
            if (binary(bytes)) continue;
            List<String> lines = new String(bytes, StandardCharsets.ISO_8859_1).lines().toList();
            String reason = rules.reason(path, lines);
            if (reason == null) source.add(new Inventory.Source(path, language, (int) lines.stream().filter(l -> !l.isBlank()).count()));
            else aside.add(new Inventory.Aside(path, language, reason));
        }
        return new Inventory(source, aside);
    }

    /** Every regular file under root, `.git` skipped and no link followed, sorted by path. */
    private static List<Path> files(Path root) {
        List<Path> found = new ArrayList<>();
        try {
            Files.walkFileTree(root, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    return dir.getFileName() != null && dir.getFileName().toString().equals(".git") ? FileVisitResult.SKIP_SUBTREE : FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (attrs.isRegularFile()) found.add(file);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        found.sort(Comparator.comparing(p -> root.relativize(p).toString().replace('\\', '/')));
        return found;
    }

    private static String gitattributes(Path root) {
        Path file = root.resolve(".gitattributes");
        return Files.isRegularFile(file) && !Files.isSymbolicLink(file) ? new String(bytes(file), StandardCharsets.ISO_8859_1) : "";
    }

    // Unsure: a very large file is read whole; a cap belongs with the sandbox's limits (TODO.md 1.5).
    private static byte[] bytes(Path file) {
        try {
            return Files.readAllBytes(file);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** A file with a zero byte in its first 8,000 is binary, as git decides it. */
    private static boolean binary(byte[] bytes) {
        for (int i = 0; i < Math.min(bytes.length, 8000); i++) if (bytes[i] == 0) return true;
        return false;
    }
}
