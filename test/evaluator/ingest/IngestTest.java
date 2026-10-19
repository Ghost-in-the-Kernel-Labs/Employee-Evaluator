package evaluator.ingest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import evaluator.rubric.Fact;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class IngestTest {
    private static final Path ROOT = Path.of(System.getProperty("evaluator.root", "."));
    private static final Path FIXTURE = ROOT.resolve("test/fixtures/small-app");
    private static final Path GOLDEN = ROOT.resolve("test/goldens/small-app/inventory.json");

    @Test
    void theFixturesInventoryIsItsGolden() throws IOException {
        String json = Ingest.read(FIXTURE).toJson();
        if (Boolean.getBoolean("evaluator.writeGoldens")) Files.writeString(GOLDEN, json);
        assertEquals(Files.readString(GOLDEN), json, "the inventory changed: if that is intended, `gradle test -PwriteGoldens` and review the diff (E-018)");
    }

    @Test
    void eachWayOfSettingAsideIsUsed() {
        Map<String, String> reasons = Ingest.read(FIXTURE).setAside().stream()
                .collect(Collectors.toMap(Inventory.Aside::path, Inventory.Aside::reason));
        assertEquals("vendored: folder vendor/", reasons.get("vendor/left_pad.js"));
        assertEquals("vendored: folder node_modules/", reasons.get("node_modules/tiny/index.js"));
        assertEquals("generated: folder dist/", reasons.get("dist/bundle.js"));
        assertEquals("vendored: .gitattributes assets/legacy.js", reasons.get("assets/legacy.js"));
        assertEquals("generated: name *_pb2.py", reasons.get("src/app/schema_pb2.py"));
        assertEquals("generated: name *.generated.ts", reasons.get("src/web/api.generated.ts"));
        assertEquals("generated: header \"@generated\"", reasons.get("src/web/client.ts"));
    }

    @Test
    void notSourceAndBinaryAreNotInTheInventory() {
        Inventory inventory = Ingest.read(FIXTURE);
        List<String> all = inventory.source().stream().map(Inventory.Source::path).collect(Collectors.toList());
        inventory.setAside().forEach(a -> all.add(a.path()));
        assertTrue(all.stream().noneMatch(p -> p.endsWith(".md") || p.endsWith(".json") || p.endsWith("blob.c")), all.toString());
    }

    @Test
    void blankLinesAreNotCounted() {
        Inventory.Source main = Ingest.read(FIXTURE).source().stream().filter(s -> s.path().equals("src/app/main.py")).findFirst().orElseThrow();
        assertEquals(6, main.lines());
        assertEquals("Python", main.language());
    }

    @Test
    void theSizeFactsCountTheApplicantsSourceOnly() {
        Map<String, Object> values = Size.facts(Ingest.read(FIXTURE)).stream().collect(Collectors.toMap(Fact::kind, Fact::value));
        assertEquals(Map.of("SIZE.source_lines", 23, "SIZE.source_files", 5, "SIZE.source_folders", 4, "SIZE.set_aside_files", 7), values);
        Fact folders = Size.facts(Ingest.read(FIXTURE)).get(2);
        assertEquals(List.of("lib", "src/app", "src/web", "tests"), folders.evidence().sources());
        assertEquals(Size.TOOL, folders.evidence().tool());
    }

    @Test
    void anEmptyRepositoryStillHasEvidence(@TempDir Path empty) {
        List<Fact> facts = Size.facts(Ingest.read(empty));
        assertEquals(0, facts.get(0).value());
        assertEquals(List.of("(none found)"), facts.get(0).evidence().sources());
        assertEquals("{\n  \"source\": [],\n  \"setAside\": []\n}\n", Ingest.read(empty).toJson());
    }

    @Test
    void noLinkIsFollowedAndGitIsNotRead(@TempDir Path repo, @TempDir Path outside) throws IOException {
        Files.writeString(outside.resolve("secret.py"), "x = 1\n");
        Files.createSymbolicLink(repo.resolve("linked.py"), outside.resolve("secret.py"));
        Files.createSymbolicLink(repo.resolve("linked_dir"), outside);
        Files.createDirectories(repo.resolve(".git/hooks"));
        Files.writeString(repo.resolve(".git/hooks/hook.py"), "y = 2\n");
        Files.writeString(repo.resolve("own.py"), "z = 3\n");
        assertEquals(List.of(new Inventory.Source("own.py", "Python", 1)), Ingest.read(repo).source());
    }

    @Test
    void aHostileFileNameIsEscapedInTheJson() {
        assertEquals("\"a\\\"b\\\\c\\u000ad\"", Inventory.quote("a\"b\\c\nd"));
    }
}
