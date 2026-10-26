package evaluator.ingest;

import evaluator.rubric.Json;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * A judge's final answer about one repository, as the kernel reads it (TODO.md 2.2j): a wrapper object round the answer of
 * agent-runner/schema/judgment.schema.json (the 2.2d shape), naming the prompt that asked and the models that answered.
 * Exactly three members: `promptHash` (64 lower-case hex digits), `models` (one to eight model ids) and `judgment` (the answer).
 * The folders say which files are counted (2.2j); the kind, the Ambition, the Quality and the Quality review are kept as the answer gave them, for `JudgedRules` (2.2k), which checks the members it reads. The file is the judge's
 * text and so data, never instructions (E-013, E-017): it is parsed, never run, and no model is called (E-003). Whatever else
 * does not match is refused with a message that says what, so a wrong file is never half-read.
 */
// Decision: E-003
public record Judgment(String promptHash, List<String> models, String kind, List<OwnCode.Folder> folders,
                       int quality, Map<String, Object> ambition, Map<String, Object> qualityReview) {
    /** The biggest judgment file read, in characters; a real answer is a few thousand. */
    public static final int MAX_CHARS = 1_000_000;
    static final List<String> KINDS = List.of("generated", "clone", "plan only", "mixed", "own-work", "built");
    private static final Pattern HASH = Pattern.compile("[0-9a-f]{64}");
    private static final Pattern MODEL = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._:-]{0,79}");
    private static final int MAX_MODELS = 8;
    private static final int VERDICT_MAX = 400;

    public Judgment {
        models = List.copyOf(models);
        folders = List.copyOf(folders);
        ambition = java.util.Collections.unmodifiableMap(new java.util.LinkedHashMap<>(ambition));
        qualityReview = java.util.Collections.unmodifiableMap(new java.util.LinkedHashMap<>(qualityReview));
    }

    // Unsure: rubric/'s Json keeps the last of a member given twice, where the harness's parser refuses it; a hand-edited file could differ from what the harness accepted.
    /** The judgment the text holds; anything else is an IllegalArgumentException saying why. */
    public static Judgment read(String text) {
        if (text.length() > MAX_CHARS) throw new IllegalArgumentException("a judgment file is at most " + MAX_CHARS + " characters, this one has " + text.length());
        Object parsed;
        try {
            parsed = Json.parse(text);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("not valid JSON (" + e.getMessage() + ")");
        }
        Map<String, Object> top = object(parsed, "the file");
        shapeOf(top);
        String hash = string(top.get("promptHash"), "promptHash");
        if (!HASH.matcher(hash).matches()) throw new IllegalArgumentException("promptHash must be 64 lower-case hex digits, not " + shown(hash));
        List<String> models = models(top.get("models"));
        Map<String, Object> answer = answer(top.get("judgment"));
        Map<String, Object> scores = object(answer.get("scores"), "judgment.scores");
        Object review = scores.get("quality");
        return new Judgment(hash, models, string(answer.get("kind"), "judgment.kind"), folders(answer.get("folders")),
                ((Double) answer.get("quality")).intValue(), object(answer.get("ambition"), "judgment.ambition"),
                review instanceof Map<?, ?> ? object(review, "judgment.scores.quality") : Map.of());
    }

    /** The 2.2d answer: its six members present and no other, its kind one of the six, the members the kernel does not act on of their type. */
    private static Map<String, Object> answer(Object value) {
        Map<String, Object> answer = object(value, "judgment");
        require(answer, List.of("kind", "folders", "scores", "ambition", "quality", "verdict"), "judgment");
        String kind = string(answer.get("kind"), "judgment.kind");
        if (!KINDS.contains(kind)) throw new IllegalArgumentException("judgment.kind is " + shown(kind) + ", one of " + String.join(", ", KINDS) + " expected");
        object(answer.get("scores"), "judgment.scores");
        object(answer.get("ambition"), "judgment.ambition");
        if (string(answer.get("verdict"), "judgment.verdict").length() > VERDICT_MAX) throw new IllegalArgumentException("judgment.verdict is longer than " + VERDICT_MAX + " characters");
        if (!(answer.get("quality") instanceof Double quality) || quality != Math.rint(quality) || quality < 1 || quality > 5) {
            throw new IllegalArgumentException("judgment.quality must be a whole number from 1 to 5");
        }
        return answer;
    }

    /** Refuses a file that is not the wrapper, naming the likelier mistakes: a bare answer, or an ensemble's result. */
    private static void shapeOf(Map<String, Object> top) {
        Set<String> want = Set.of("promptHash", "models", "judgment");
        if (top.keySet().equals(want)) return;
        if (top.containsKey("kind") && top.containsKey("folders")) {
            throw new IllegalArgumentException("this is a bare judgment answer; --judged takes it wrapped as {\"promptHash\": ..., \"models\": [...], \"judgment\": {the answer}}, so the card can name the prompt and the models");
        }
        if (top.containsKey("fields") || top.containsKey("status")) {
            throw new IllegalArgumentException("this is an ensemble result (status, fields, folders); --judged takes the settled answer wrapped as {\"promptHash\": ..., \"models\": [...], \"judgment\": {the answer}}");
        }
        List<String> missing = new ArrayList<>(), extra = new ArrayList<>();
        for (String key : List.of("promptHash", "models", "judgment")) if (!top.containsKey(key)) missing.add(key);
        for (String key : top.keySet()) if (!want.contains(key)) extra.add(shown(key));
        throw new IllegalArgumentException("the file must hold exactly promptHash, models and judgment" + (missing.isEmpty() ? "" : "; missing " + String.join(", ", missing))
                + (extra.isEmpty() ? "" : "; not allowed " + String.join(", ", extra)));
    }

    /** The models that answered: one to eight distinct ids of letters, digits and `. _ : -`. */
    private static List<String> models(Object value) {
        if (!(value instanceof List<?> list) || list.isEmpty() || list.size() > MAX_MODELS) {
            throw new IllegalArgumentException("models must be a list of one to " + MAX_MODELS + " model ids");
        }
        List<String> models = new ArrayList<>();
        for (Object item : list) {
            String id = string(item, "models item");
            if (!MODEL.matcher(id).matches()) throw new IllegalArgumentException("models holds " + shown(id) + ", which is not a model id (letters, digits, . _ : -)");
            if (models.contains(id)) throw new IllegalArgumentException("models lists " + shown(id) + " twice");
            models.add(id);
        }
        return models;
    }

    /** The answer's folders, each a path and one of the eight classes, no path twice. */
    private static List<OwnCode.Folder> folders(Object value) {
        if (!(value instanceof List<?> list)) throw new IllegalArgumentException("judgment.folders must be a list");
        List<OwnCode.Folder> folders = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < list.size(); i++) {
            String where = "judgment.folders[" + i + "]";
            Map<String, Object> folder = object(list.get(i), where);
            require(folder, List.of("path", "class"), where);
            String path = path(string(folder.get("path"), where + ".path"), where + ".path");
            String title = string(folder.get("class"), where + ".class");
            OwnCode.FolderClass folderClass = OwnCode.FolderClass.of(title);
            if (folderClass == null) throw new IllegalArgumentException(where + ".class is " + shown(title) + ", not one of the eight classes");
            if (!seen.add(path)) throw new IllegalArgumentException(where + ".path " + shown(path) + " is listed twice");
            folders.add(new OwnCode.Folder(path, folderClass));
        }
        return folders;
    }

    /** A folder path from the root with `/`: `.` for the root, one trailing slash tolerated, nothing absolute, empty or climbing. */
    static String path(String path, String where) {
        String p = path.endsWith("/") ? path.substring(0, path.length() - 1) : path;
        boolean bad = p.isEmpty() || p.startsWith("/") || p.contains("\\");
        for (String part : p.split("/", -1)) if (part.isEmpty() || part.equals("..") || (part.equals(".") && !p.equals("."))) bad = true;
        if (bad) throw new IllegalArgumentException(where + " " + shown(path) + " must be relative to the root with `/`, non-empty and without `..`, `.` parts or empty parts");
        return p;
    }

    private static void require(Map<String, Object> object, List<String> keys, String where) {
        List<String> missing = keys.stream().filter(k -> !object.containsKey(k)).toList();
        List<String> extra = object.keySet().stream().filter(k -> !keys.contains(k)).map(Judgment::shown).toList();
        if (!missing.isEmpty() || !extra.isEmpty()) {
            throw new IllegalArgumentException(where + " must hold exactly " + String.join(", ", keys) + (missing.isEmpty() ? "" : "; missing " + String.join(", ", missing))
                    + (extra.isEmpty() ? "" : "; not allowed " + String.join(", ", extra)));
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> object(Object value, String where) {
        if (!(value instanceof Map<?, ?>)) throw new IllegalArgumentException(where + " must be a JSON object");
        return (Map<String, Object>) value;
    }

    private static String string(Object value, String where) {
        if (!(value instanceof String s)) throw new IllegalArgumentException(where + " must be a string");
        return s;
    }

    /** A value of the file quoted for a message, cut short: it is the judge's text. */
    private static String shown(String value) {
        String cut = value.length() > 60 ? value.substring(0, 60) + "..." : value;
        return Json.quote(cut);
    }
}
