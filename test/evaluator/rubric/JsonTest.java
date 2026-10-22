package evaluator.rubric;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** The kernel's own JSON reader, every value worked by hand (TODO.md 1.12: its mutants, run 1). */
class JsonTest {
    @Test
    void everyKindOfValue() {
        assertEquals("a", Json.parse("\"a\""));
        assertEquals(12.0, Json.parse("12"));
        assertEquals(-1.5e3, Json.parse("-1.5e3"));
        assertEquals(Boolean.TRUE, Json.parse("true"));
        assertEquals(Boolean.FALSE, Json.parse("false"));
        assertNull(Json.parse("null"));
        assertEquals(List.of(), Json.parse("[]"));
        assertEquals(Map.of(), Json.parse("{}"));
        assertEquals(List.of(), Json.parse("[ ]"));
        assertEquals(Map.of(), Json.parse("{ }"));
        assertEquals(100000.0, Json.parse("1e+5"));
        assertEquals(0.025, Json.parse("2.5E-2"));
    }

    @Test
    void anObjectKeepsItsKeysInOrderAndNests() {
        Object parsed = Json.parse(" { \"z\" : 1 , \"a\" : [ true , null , \"x\" ] , \"m\" : { \"k\" : { } } } ");
        Map<String, Object> expected = new LinkedHashMap<>();
        expected.put("z", 1.0);
        List<Object> items = new ArrayList<>();
        items.add(true);
        items.add(null);
        items.add("x");
        expected.put("a", items);
        expected.put("m", Map.of("k", Map.of()));
        assertEquals(expected, parsed);
        assertEquals(List.of("z", "a", "m"), List.copyOf(((Map<?, ?>) parsed).keySet()));
    }

    @Test
    void whitespaceAnywhereBetweenTokens() {
        assertEquals(List.of(1.0, 2.0), Json.parse("\n\t[\r\n1\t,\n 2 ]\n"));
        assertEquals(Map.of("a", "b"), Json.parse("{\"a\":\"b\"}"));
    }

    @Test
    void escapesAreUndone() {
        assertEquals("a\"b\\c/d", Json.parse("\"a\\\"b\\\\c\\/d\""));
        assertEquals("\n\t\r\b\f", Json.parse("\"\\n\\t\\r\\b\\f\""));
        assertEquals("é!", Json.parse("\"\\u00e9\\u0021\""));
        assertEquals("ab", Json.parse("\"a\\u0062\""));
    }

    @Test
    void malformedInputIsAnIllegalArgumentNamingItsOffset() {
        for (String bad : List.of("", "   ", "[1,", "[1 2]", "{\"a\" 1}", "{\"a\":1,}", "{a:1}", "\"open", "tru", "nul", "fals",
                "@", "1 2", "[1]]", "\"\\u12\"", "--", "{\"a\":1", "[")) {
            IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> Json.parse(bad), bad);
            assertTrue(e.getMessage().contains(" at offset "), bad + ": " + e.getMessage());
        }
    }

    @Test
    void theErrorSaysWhatAndWhere() {
        assertEquals("trailing text at offset 2", message("1 x"));
        assertEquals("expected ':' at offset 5", message("{\"a\" 1}"));
        assertEquals("expected '}' at offset 7", message("{\"a\":1 2}"));
        assertEquals("expected ']' at offset 3", message("[1 2]"));
        assertEquals("expected true at offset 0", message("trUe"));
        assertEquals("unexpected character at offset 0", message("@"));
        assertEquals("unexpected end at offset 1", message("["));
    }

    @Test
    void nestingIsCappedSoAReportCannotBlowTheStack() {
        String deepest = "[".repeat(Json.MAX_DEPTH) + "]".repeat(Json.MAX_DEPTH);
        assertTrue(Json.parse(deepest) instanceof List<?>);
        String objects = "{\"a\":".repeat(Json.MAX_DEPTH - 1) + "{}" + "}".repeat(Json.MAX_DEPTH - 1);
        assertTrue(Json.parse(objects) instanceof Map<?, ?>);
        assertEquals("nested deeper than 256 at offset 256",
                message("[".repeat(Json.MAX_DEPTH + 1) + "]".repeat(Json.MAX_DEPTH + 1)));
        assertEquals("nested deeper than 256 at offset " + 5 * Json.MAX_DEPTH,
                message("{\"a\":".repeat(Json.MAX_DEPTH) + "{}" + "}".repeat(Json.MAX_DEPTH)));
    }

    @Test
    void depthIsGivenBackWhenAContainerCloses() {
        String siblings = ("[".repeat(Json.MAX_DEPTH - 1) + "]".repeat(Json.MAX_DEPTH - 1) + ",").repeat(3);
        String wide = "[" + siblings.substring(0, siblings.length() - 1) + "]";
        assertEquals(3, ((List<?>) Json.parse(wide)).size());
        String emptySiblings = "[" + "[],".repeat(300) + "{}," .repeat(300) + "[]]";
        assertEquals(601, ((List<?>) Json.parse(emptySiblings)).size());
        String full = "{\"a\":".repeat(Json.MAX_DEPTH - 2) + "1" + "}".repeat(Json.MAX_DEPTH - 2);
        String fullSiblings = "[" + (full + ",").repeat(2) + full + "]";
        assertEquals(3, ((List<?>) Json.parse(fullSiblings)).size());
        String lists = "[1,".repeat(Json.MAX_DEPTH - 2) + "1" + "]".repeat(Json.MAX_DEPTH - 2);
        assertEquals(3, ((List<?>) Json.parse("[" + lists + "," + lists + "," + lists + "]")).size());
    }

    private static String message(String text) {
        return assertThrows(IllegalArgumentException.class, () -> Json.parse(text)).getMessage();
    }
}
