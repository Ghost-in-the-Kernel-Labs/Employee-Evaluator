package evaluator.analyze;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class TokensTest {
    private static final String STR = "\"\"";

    /** The lines of a block comment are counted: `b` follows two newlines inside the comment, so it is on line 3. */
    @Test
    void aBlockCommentOverSeveralLinesMovesTheLineCount() {
        Tokens.List2 t = Tokens.of("a /* one\ntwo\nthree */ b", false);
        assertEquals(List.of("$", "$"), t.tokens());
        assertEquals(List.of(1, 3), t.lines());
    }

    // In "x */* c */ y" a star and a slash precede the comment that opens at index 3: that close is not its own, the one at index 8 is.
    @Test
    void aBlockCommentEndsAtItsOwnCloseNotAnEarlierOne() {
        assertEquals(List.of("$", "*", "$"), Tokens.of("x */* c */ y", false).tokens());
    }

    @Test
    void anUnclosedBlockCommentRunsToTheEnd() {
        Tokens.List2 t = Tokens.of("a /* b\nc", false);
        assertEquals(List.of("$"), t.tokens());
    }

    /** An unclosed string runs to the end of the text, whatever the quote: one token, nothing after. */
    @Test
    void aStringWithNoClosingQuoteRunsToTheEndOfTheText() {
        assertEquals(List.of("$", "=", STR), Tokens.of("x = \"abc", false).tokens());
        assertEquals(List.of(STR), Tokens.of("`ab\ncd", false).tokens());
        assertEquals(List.of(STR), Tokens.of("\"\"\"ab\ncd\"\"", true).tokens(), "two quotes do not close three");
        assertEquals(List.of(STR), Tokens.of("\"ab\\", false).tokens(), "an escape at the very end");
    }

    /** `\"` does not close: `"a\"b" c` is one string and `c`, not a string, `b`, a string. */
    @Test
    void anEscapedQuoteDoesNotCloseAString() {
        assertEquals(List.of(STR, "$"), Tokens.of("\"a\\\"b\" c", false).tokens());
        assertEquals(List.of(STR, "$"), Tokens.of("'a\\\\' c", false).tokens(), "an escaped backslash, then the quote closes");
    }

    /** `'` and `"` end at a newline (an unclosed string cannot swallow the file); `x` is on line 2. */
    @Test
    void aSingleOrDoubleQuotedStringEndsAtANewline() {
        for (String quote : List.of("'", "\"")) {
            Tokens.List2 t = Tokens.of(quote + "abc\nx", false);
            assertEquals(List.of(STR, "$"), t.tokens(), quote);
            assertEquals(List.of(1, 2), t.lines(), quote);
        }
    }

    /** A backtick string and Python's triple quotes run over newlines: one token each, the next token on line 2. */
    @Test
    void aBacktickOrTripleQuotedStringRunsOverNewlines() {
        Tokens.List2 back = Tokens.of("`a\nb` c", false);
        assertEquals(List.of(STR, "$"), back.tokens());
        assertEquals(List.of(1, 2), back.lines());
        Tokens.List2 triple = Tokens.of("\"\"\"a\nb\"\"\" c", true);
        assertEquals(List.of(STR, "$"), triple.tokens());
        assertEquals(List.of(1, 2), triple.lines());
        assertEquals(List.of(STR, "$"), Tokens.of("'''a\nb''' c", true).tokens());
    }
}
