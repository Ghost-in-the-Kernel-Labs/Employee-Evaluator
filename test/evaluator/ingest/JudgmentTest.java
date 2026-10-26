package evaluator.ingest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import evaluator.ingest.OwnCode.Folder;
import evaluator.ingest.OwnCode.FolderClass;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

/** The judgment file the kernel accepts (TODO.md 2.2j): the 2.2d answer in a wrapper naming the prompt and the models. */
class JudgmentTest {
    private static final Path ROOT = Path.of(System.getProperty("evaluator.root", "."));
    private static final String HASH = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";
    private static final String ANSWER = """
            {"kind": "own-work", "folders": %s,
             "scores": {"quality": {}, "complexity": {}, "navigability": {}}, "ambition": {}, "quality": 3, "verdict": "ok"}""";

    private static String file(String hash, String models, String answer) {
        return "{\"promptHash\": " + hash + ", \"models\": " + models + ", \"judgment\": " + answer + "}";
    }

    private static String wrapped(String folders) {
        return file("\"" + HASH + "\"", "[\"m-1\"]", ANSWER.formatted(folders));
    }

    private static String refusal(String text) {
        return assertThrows(IllegalArgumentException.class, () -> Judgment.read(text), text).getMessage();
    }

    @Test
    void theFixturesJudgmentIsRead() throws Exception {
        Judgment j = Judgment.read(Files.readString(ROOT.resolve("test/fixtures/judged/judgment.json")));
        assertEquals(HASH, j.promptHash());
        assertEquals(List.of("synthetic-haiku-1", "synthetic-opus-1"), j.models());
        assertEquals("own-work", j.kind());
        assertEquals(7, j.folders().size());
        assertEquals(new Folder("docs/tools", FolderClass.OWN_CODE), j.folders().get(6));
    }

    @Test
    void everyKindIsAccepted() {
        for (String kind : List.of("generated", "clone", "plan only", "mixed", "own-work", "built")) {
            assertEquals(kind, Judgment.read(wrapped("[]").replace("own-work", kind)).kind());
        }
        assertTrue(refusal(wrapped("[]").replace("own-work", "tidy")).contains("judgment.kind is \"tidy\""));
    }

    /** The two likeliest wrong files are named for what they are, not just refused. */
    @Test
    void aBareAnswerAndAnEnsembleResultAreNamed() {
        assertTrue(refusal(ANSWER.formatted("[]")).contains("bare judgment answer"));
        assertTrue(refusal("{\"status\": \"ok\", \"fields\": {}, \"folders\": {}}").contains("ensemble result"));
        assertTrue(refusal("{\"fields\": {}}").contains("ensemble result"));
        assertTrue(refusal("{\"status\": \"ok\"}").contains("ensemble result"));
    }

    @Test
    void theWrapperHoldsExactlyItsThreeMembers() {
        assertEquals("the file must hold exactly promptHash, models and judgment; missing models, judgment; not allowed \"extra\"",
                refusal("{\"promptHash\": \"" + HASH + "\", \"extra\": 1}"));
        assertTrue(refusal(wrapped("[]").replace("\"models\"", "\"extra\": 1, \"models\"")).contains("not allowed \"extra\""));
        assertEquals("the file must hold exactly promptHash, models and judgment; missing judgment",
                refusal("{\"promptHash\": \"" + HASH + "\", \"models\": [\"m\"]}"));
        String whole = wrapped("[]");
        assertEquals("the file must hold exactly promptHash, models and judgment; not allowed \"x\"", refusal(whole.substring(0, whole.length() - 1) + ", \"x\": 1}"));
    }

    @Test
    void notJsonAndNotAnObjectAreRefused() {
        assertTrue(refusal("not json").startsWith("not valid JSON ("));
        assertEquals("the file must be a JSON object", refusal("[]"));
        assertTrue(refusal("{\"promptHash\": \"" + HASH + "\", \"models\": [\"m\"], \"judgment\": []}").contains("judgment must be a JSON object"));
        assertTrue(refusal("x".repeat(Judgment.MAX_CHARS + 1)).contains("at most " + Judgment.MAX_CHARS));
        Judgment.read(wrapped("[]") + " ".repeat(Judgment.MAX_CHARS - wrapped("[]").length()));
    }

    @Test
    void thePromptHashIsSixtyFourLowerCaseHexDigits() {
        for (String bad : List.of("\"\"", "\"" + HASH.toUpperCase() + "\"", "\"" + HASH + "0\"", "\"" + HASH.substring(1) + "\"", "\"sha256 " + HASH + "\"", "\"" + HASH.replace('0', 'g') + "\"")) {
            assertTrue(refusal(file(bad, "[\"m\"]", ANSWER.formatted("[]"))).startsWith("promptHash must be 64 lower-case hex digits"), bad);
        }
        assertTrue(refusal(file("7", "[\"m\"]", ANSWER.formatted("[]"))).contains("promptHash must be a string"));
    }

    @Test
    void modelsAreOneToEightDistinctIds() {
        for (String bad : List.of("[]", "\"m\"", "[1]", "[\"\"]", "[\"has space\"]", "[\"-lead\"]", "[\"a\", \"a\"]", "[\"1\",\"2\",\"3\",\"4\",\"5\",\"6\",\"7\",\"8\",\"9\"]", "[\"" + "m".repeat(81) + "\"]")) {
            assertTrue(assertThrows(IllegalArgumentException.class, () -> Judgment.read(file("\"" + HASH + "\"", bad, ANSWER.formatted("[]"))), bad).getMessage().contains("model"), bad);
        }
        assertEquals(8, Judgment.read(file("\"" + HASH + "\"", "[\"1\",\"2\",\"3\",\"4\",\"5\",\"6\",\"7\",\"8\"]", ANSWER.formatted("[]"))).models().size());
        assertEquals(80, Judgment.read(file("\"" + HASH + "\"", "[\"" + "m".repeat(80) + "\"]", ANSWER.formatted("[]"))).models().get(0).length());
        assertEquals(List.of("claude-haiku-5-5", "a.b_c:d"), Judgment.read(file("\"" + HASH + "\"", "[\"claude-haiku-5-5\", \"a.b_c:d\"]", ANSWER.formatted("[]"))).models());
    }

    @Test
    void theAnswerHoldsTheSixMembersOfTheSchema() {
        String answer = ANSWER.formatted("[]");
        assertTrue(refusal(file("\"" + HASH + "\"", "[\"m\"]", answer.replace("\"verdict\": \"ok\"", "\"x\": 1"))).contains("judgment must hold exactly kind, folders, scores, ambition, quality, verdict; missing verdict; not allowed \"x\""));
        assertTrue(refusal(file("\"" + HASH + "\"", "[\"m\"]", answer.replace("\"scores\": {\"quality\": {}, \"complexity\": {}, \"navigability\": {}}", "\"scores\": 1"))).contains("judgment.scores must be a JSON object"));
        assertTrue(refusal(file("\"" + HASH + "\"", "[\"m\"]", answer.replace("\"ambition\": {}", "\"ambition\": []"))).contains("judgment.ambition must be a JSON object"));
        assertTrue(refusal(file("\"" + HASH + "\"", "[\"m\"]", answer.replace("\"verdict\": \"ok\"", "\"verdict\": 5"))).contains("judgment.verdict must be a string"));
        assertTrue(refusal(file("\"" + HASH + "\"", "[\"m\"]", answer.replace("\"verdict\": \"ok\"", "\"verdict\": \"" + "v".repeat(401) + "\""))).contains("longer than 400"));
        Judgment.read(file("\"" + HASH + "\"", "[\"m\"]", answer.replace("\"verdict\": \"ok\"", "\"verdict\": \"" + "v".repeat(400) + "\"")));
        for (String quality : List.of("0", "6", "2.5", "\"3\"", "null")) {
            assertTrue(refusal(file("\"" + HASH + "\"", "[\"m\"]", answer.replace("\"quality\": 3", "\"quality\": " + quality))).contains("judgment.quality must be a whole number from 1 to 5"), quality);
        }
        for (String quality : List.of("1", "5")) Judgment.read(file("\"" + HASH + "\"", "[\"m\"]", answer.replace("\"quality\": 3", "\"quality\": " + quality)));
    }

    @Test
    void aFolderIsAPathAndOneOfTheEightClasses() {
        Judgment ok = Judgment.read(wrapped("[{\"path\": \"src\", \"class\": \"own code\"}, {\"path\": \".\", \"class\": \"docs\"}, {\"path\": \"a/b/\", \"class\": \"fixture data\"}]"));
        assertEquals(List.of(new Folder("src", FolderClass.OWN_CODE), new Folder(".", FolderClass.DOCS), new Folder("a/b", FolderClass.FIXTURE_DATA)), ok.folders());
        assertTrue(refusal(wrapped("[1]")).contains("judgment.folders[0] must be a JSON object"));
        assertTrue(refusal(wrapped("{}")).contains("judgment.folders must be a list"));
        assertTrue(refusal(wrapped("[{\"path\": \"a\"}]")).contains("judgment.folders[0] must hold exactly path, class; missing class"));
        assertTrue(refusal(wrapped("[{\"path\": \"a\", \"class\": \"tests\"}]")).contains("judgment.folders[0].class is \"tests\", not one of the eight classes"));
        assertTrue(refusal(wrapped("[{\"path\": 1, \"class\": \"docs\"}]")).contains("judgment.folders[0].path must be a string"));
        assertTrue(refusal(wrapped("[{\"path\": \"a\", \"class\": \"docs\"}, {\"path\": \"b\", \"class\": \"docs\"}, {\"path\": \"a/\", \"class\": \"own code\"}]")).contains("judgment.folders[2].path \"a\" is listed twice"));
    }

    @Test
    void aFolderPathStaysInsideTheRepository() {
        for (String bad : List.of("", "/", "/abs", "../up", "a/../b", "a/..", "./a", "a/./b", "a//b", "a\\\\b", "//", "a//", "././")) {
            String message = refusal(wrapped("[{\"path\": \"" + bad + "\", \"class\": \"docs\"}]"));
            assertTrue(message.startsWith("judgment.folders[0].path ") && message.contains("must be relative to the root"), bad + " -> " + message);
        }
        assertEquals("a", Judgment.path("a/", "w"), "one trailing slash is tolerated");
        assertEquals(".", Judgment.path(".", "w"));
        assertEquals(".", Judgment.path("./", "w"), "the root with a trailing slash is the root");
        assertEquals("a..b", Judgment.path("a..b", "w"));
        assertEquals("..a", Judgment.path("..a", "w"));
        assertEquals("a.b/.c", Judgment.path("a.b/.c", "w"));
    }

    @Test
    void aMessageQuotesTheJudgesTextCutShort() {
        String long60 = "p".repeat(80);
        String message = refusal(wrapped("[{\"path\": \"a\", \"class\": \"" + long60 + "\"}]"));
        assertTrue(message.contains("\"" + "p".repeat(60) + "...\""), message);
        assertEquals(-1, message.indexOf("p".repeat(61)));
        assertTrue(refusal(wrapped("[{\"path\": \"a\", \"class\": \"" + "q".repeat(60) + "\"}]")).contains("\"" + "q".repeat(60) + "\""), "exactly 60 is not cut");
    }
}
