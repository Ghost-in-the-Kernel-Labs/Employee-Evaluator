import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The smallest JSON reader the doc tests need (objects, arrays, strings, numbers, booleans, null),
 * so docs/modules.json is read with no library until phase 0.3 vendors one.
 */
final class Json {
    private final String text;
    private int at;

    private Json(String text) {
        this.text = text;
    }

    static Object parse(String text) {
        Json json = new Json(text);
        Object value = json.value();
        json.space();
        if (json.at != text.length()) throw json.error("trailing text");
        return value;
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
        at++;
        space();
        if (peek('}')) return map;
        do {
            space();
            String key = string();
            space();
            expect(':');
            map.put(key, value());
            space();
        } while (take(','));
        expect('}');
        return map;
    }

    private List<Object> array() {
        List<Object> list = new ArrayList<>();
        at++;
        space();
        if (peek(']')) return list;
        do {
            list.add(value());
            space();
        } while (take(','));
        expect(']');
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

    private IllegalArgumentException error(String what) {
        return new IllegalArgumentException(what + " at offset " + at);
    }
}
