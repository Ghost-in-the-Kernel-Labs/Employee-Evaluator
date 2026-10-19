package evaluator.analyze;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Source text as normalized tokens, for duplication (E-036): comments dropped, every name that is not
 * a keyword one token (`$`), every string one token (`""`), every number one token (`0`), keywords and
 * punctuation as written. A renamed copy reads the same as its original; a different shape does not.
 * One pass, no backtracking: a hostile file costs its length and nothing more.
 */
final class Tokens {
    /** The keywords of Java, TypeScript and Python, kept as written so control flow is not normalized away. */
    private static final Set<String> KEYWORDS = Set.of(
            "abstract", "and", "as", "assert", "async", "await", "boolean", "break", "byte", "case", "catch", "char", "class",
            "const", "continue", "def", "default", "del", "delete", "do", "double", "elif", "else", "enum", "except", "export",
            "extends", "false", "False", "final", "finally", "float", "for", "from", "function", "global", "if", "implements",
            "import", "in", "instanceof", "int", "interface", "is", "lambda", "let", "long", "match", "new", "None", "nonlocal",
            "not", "null", "or", "pass", "private", "protected", "public", "raise", "return", "short", "static", "super",
            "switch", "this", "throw", "throws", "true", "True", "try", "type", "typeof", "undefined", "var", "void", "while",
            "with", "yield");

    /** Normalized tokens, each with the line (from 1) it starts on. */
    record List2(List<String> tokens, List<Integer> lines) {}

    private Tokens() {}

    /** The tokens of text; `hashComments` for Python's `#`, otherwise `//` and `/* *\/`. */
    static List2 of(String text, boolean hashComments) {
        Lexer lexer = new Lexer(text, hashComments);
        while (lexer.at < text.length()) lexer.step();
        return new List2(lexer.tokens, lexer.lines);
    }

    /** One pass over the text: where it is, the line it is on, and the tokens so far. */
    private static final class Lexer {
        private final String text;
        private final boolean hashComments;
        final List<String> tokens = new ArrayList<>();
        final List<Integer> lines = new ArrayList<>();
        int at;
        int line = 1;

        Lexer(String text, boolean hashComments) {
            this.text = text;
            this.hashComments = hashComments;
        }

        /** One token, or one stretch of space or comment, read and the position moved past it. */
        void step() {
            char c = text.charAt(at);
            if (c == '\n') {
                line++;
                at++;
            } else if (Character.isWhitespace(c)) {
                at++;
            } else if (!comment()) {
                if (c == '"' || c == '\'' || c == '`') string();
                else if (Character.isDigit(c)) add("0", skip(at, true));
                else if (Character.isLetter(c) || c == '_' || c == '$') word();
                else add(String.valueOf(c), at + 1);
            }
        }

        /** A comment skipped, if one starts here: `#` (Python) or `//` to the line's end, `/* *\/` to its close. */
        private boolean comment() {
            if (hashComments ? text.charAt(at) == '#' : text.startsWith("//", at)) {
                while (at < text.length() && text.charAt(at) != '\n') at++;
                return true;
            }
            if (hashComments || !text.startsWith("/*", at)) return false;
            int end = text.indexOf("*/", at + 2);
            end = end < 0 ? text.length() : end + 2;
            line += count(text, at, end);
            at = end;
            return true;
        }

        /** A string, one token on the line it opens on; the lines it spans counted after. */
        private void string() {
            int start = at;
            int end = stringEnd(text, at);
            add("\"\"", end);
            line += count(text, start, end);
        }

        private void word() {
            int end = skip(at, false);
            String word = text.substring(at, end);
            add(KEYWORDS.contains(word) ? word : "$", end);
        }

        /** Past the characters of a number (letters, digits, `.`, `_`) or of a name (letters, digits, `_`, `$`). */
        private int skip(int from, boolean number) {
            int end = from;
            while (end < text.length()) {
                char c = text.charAt(end);
                if (!(Character.isLetterOrDigit(c) || c == '_' || (number ? c == '.' : c == '$'))) break;
                end++;
            }
            return end;
        }

        /** A token on the current line, and the position moved to end. */
        private void add(String token, int end) {
            tokens.add(token);
            lines.add(line);
            at = end;
        }
    }

    /** Where a string starting at i ends: its closing quote (three for Python's `"""`), escapes skipped; the end of the text if unclosed. */
    private static int stringEnd(String text, int i) {
        char quote = text.charAt(i);
        String close = text.startsWith(String.valueOf(quote).repeat(3), i) ? String.valueOf(quote).repeat(3) : String.valueOf(quote);
        int at = i + close.length();
        while (at < text.length()) {
            if (text.charAt(at) == '\\') at += 2;
            else if (text.startsWith(close, at)) return at + close.length();
            else if (close.length() == 1 && quote != '`' && text.charAt(at) == '\n') return at;
            else at++;
        }
        return text.length();
    }

    private static int count(String text, int from, int to) {
        int lines = 0;
        for (int i = from; i < Math.min(to, text.length()); i++) if (text.charAt(i) == '\n') lines++;
        return lines;
    }
}
