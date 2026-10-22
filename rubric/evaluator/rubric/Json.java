package evaluator.rubric;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The smallest JSON reader the kernel needs (objects, arrays, strings, numbers, booleans, null), with
 * no library: GitHub's API (E-043), Jest's report of a submission's run (E-037), and, for the project's
 * own tooling and doc tests, docs/modules.json. In rubric/ because every module may import rubric/ and
 * rubric/ imports nothing of ours (MOD-02). A report is an attacker's file, so nesting is capped and every malformed
 * input is an IllegalArgumentException, never an index error or a blown stack.
 */
public final class Json {
    /** The deepest an object or array may nest. */
    static final int MAX_DEPTH = 256;
    private final String text;
    private int at;
    private int depth;

    private Json(String text) {
        this.text = text;
    }

    /** The value the text holds: a Map, a List, a String, a Double, a Boolean or null. */
    public static Object parse(String text) {
        Json json = new Json(text);
        try {
            Object value = json.value();
            json.space();
            if (json.at != text.length()) throw json.error("trailing text");
            return value;
        } catch (IndexOutOfBoundsException | NumberFormatException e) {
            throw json.error("malformed (" + e.getMessage() + ")");
        }
    }

    /** The next JSON value: an object, an array, a string, a number, true, false or null. */
    private Object value() {
        space();
        if (at >= text.length()) throw error("unexpected end");
        char c = text.charAt(at);
        return switch (c) {
            case '{' -> object();
            case '[' -> array();
            case '"' -> string();
            case 't' -> word("true", Boolean.TRUE);
            case 'f' -> word("false", Boolean.FALSE);
            case 'n' -> word("null", null);
            default -> number();
        };
    }

    /** An object, its keys in order. */
    private Map<String, Object> object() {
        Map<String, Object> map = new LinkedHashMap<>();
        deeper();
        at++;
        space();
        if (peek('}')) {
            depth--;
            return map;
        }
        do {
            space();
            String key = string();
            space();
            expect(':');
            map.put(key, value());
        } while (take(','));
        expect('}');
        depth--;
        return map;
    }

    /** An array, its items in order. */
    private List<Object> array() {
        List<Object> list = new ArrayList<>();
        deeper();
        at++;
        space();
        if (peek(']')) {
            depth--;
            return list;
        }
        do {
            list.add(value());
        } while (take(','));
        expect(']');
        depth--;
        return list;
    }

    /** A quoted string, its escapes undone. */
    private String string() {
        expect('"');
        StringBuilder out = new StringBuilder();
        while (text.charAt(at) != '"') {
            char c = text.charAt(at++);
            if (c != '\\') {
                out.append(c);
                continue;
            }
            char e = text.charAt(at++);
            switch (e) {
                case 'n' -> out.append('\n');
                case 't' -> out.append('\t');
                case 'r' -> out.append('\r');
                case 'b' -> out.append('\b');
                case 'f' -> out.append('\f');
                case 'u' -> {
                    out.append((char) Integer.parseInt(text.substring(at, at + 4), 16));
                    at += 4;
                }
                default -> out.append(e);
            }
        }
        at++;
        return out.toString();
    }

    private Object number() {
        int start = at;
        while (at < text.length() && "+-0123456789.eE".indexOf(text.charAt(at)) >= 0) at++;
        if (start == at) throw error("unexpected character");
        return Double.parseDouble(text.substring(start, at));
    }

    private Object word(String word, Object value) {
        if (!text.startsWith(word, at)) throw error("expected " + word);
        at += word.length();
        return value;
    }

    private void space() {
        while (at < text.length() && Character.isWhitespace(text.charAt(at))) at++;
    }

    private boolean peek(char c) {
        if (at < text.length() && text.charAt(at) == c) {
            at++;
            return true;
        }
        return false;
    }

    private boolean take(char c) {
        space();
        return peek(c);
    }

    private void expect(char c) {
        if (!peek(c)) throw error("expected '" + c + "'");
    }

    private void deeper() {
        if (++depth > MAX_DEPTH) throw error("nested deeper than " + MAX_DEPTH);
    }

    private IllegalArgumentException error(String what) {
        return new IllegalArgumentException(what + " at offset " + at);
    }
}
