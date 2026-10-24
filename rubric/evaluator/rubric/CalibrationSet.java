package evaluator.rubric;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

/**
 * The calibration set's loader and validator (TODO.md 2.1): one JSON file per label, named by its id, in a folder.
 * It refuses a missing, unknown or out-of-range field, a duplicate id and an unknown value, each with an IllegalArgumentException that names the file or field.
 */
// Decision: E-080
public final class CalibrationSet {
    /** How many labelled repositories the set needs before it is enforced (TODO.md 2.1). */
    public static final int TARGET = 30;
    private static final Set<String> REQUIRED = Set.of("id", "kind", "ambition", "quality");

    private CalibrationSet() {
    }

    /** Where the set stands: PENDING below the target (not a failure), MET when full. */
    public record Status(State state, String message) {
        /** The two states. */
        public enum State { PENDING, MET }
    }

    /** The label one fixture's text holds; anything else about it is an IllegalArgumentException. */
    public static Label parse(String text) {
        Object json = Json.parse(text);
        if (!(json instanceof Map<?, ?> map)) throw new IllegalArgumentException("a label is a JSON object");
        for (Object key : map.keySet()) {
            if (!REQUIRED.contains(key)) throw new IllegalArgumentException("unknown field " + key + " (known: " + new java.util.TreeSet<>(REQUIRED) + ")");
        }
        for (String key : REQUIRED) {
            if (!map.containsKey(key)) throw new IllegalArgumentException("missing field " + key);
        }
        return new Label(
                text(map, "id"),
                choose(Label.Kind.values(), text(map, "kind"), "kind"),
                choose(Label.Ambition.values(), text(map, "ambition"), "ambition"),
                quality(map.get("quality")));
    }

    /** Every label in the folder (`<id>.json` each, in id order), or none when the folder is absent; a duplicate id, or a file not named by its id, is refused. */
    public static List<Label> load(Path folder) {
        if (!Files.isDirectory(folder)) return List.of();
        Map<Path, Label> labels = new LinkedHashMap<>();
        try (Stream<Path> list = Files.list(folder)) {
            for (Path file : list.filter(p -> p.getFileName().toString().endsWith(".json")).sorted().toList()) {
                Label label;
                try {
                    label = parse(Files.readString(file));
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException(file.getFileName() + ": " + e.getMessage(), e);
                }
                if (!file.getFileName().toString().equals(label.id() + ".json")) {
                    throw new IllegalArgumentException(file.getFileName() + ": a label's file is named by its id, " + label.id() + ".json");
                }
                labels.put(file, label);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return of(new ArrayList<>(labels.values()));
    }

    /** The same labels, once no id repeats. */
    public static List<Label> of(List<Label> labels) {
        Set<String> seen = new HashSet<>();
        for (Label label : labels) {
            if (!seen.add(label.id())) throw new IllegalArgumentException("duplicate id " + label.id());
        }
        return List.copyOf(labels);
    }

    /** Where the set stands against "30 labelled": PENDING until the count reaches the target, then MET. */
    public static Status status(List<Label> labels) {
        if (labels.size() < TARGET) {
            return new Status(Status.State.PENDING, labels.size() + " of " + TARGET + " repositories labelled (waits on Ghost's labels)");
        }
        return new Status(Status.State.MET, labels.size() + " labelled");
    }

    private static String text(Map<?, ?> map, String key) {
        return map.get(key) instanceof String s ? s : fail("field " + key + " is text");
    }

    private static <E extends Enum<E>> E choose(E[] values, String wire, String field) {
        for (E value : values) {
            if (Label.wire(value).equals(wire)) return value;
        }
        throw new IllegalArgumentException(field + " " + wire + " is not one of " + Label.wires(values));
    }

    private static int quality(Object value) {
        if (!(value instanceof Double d) || d != Math.rint(d)) throw new IllegalArgumentException("field quality is a whole number, " + Label.QUALITY_MIN + "–" + Label.QUALITY_MAX);
        if (d < Label.QUALITY_MIN || d > Label.QUALITY_MAX) throw new IllegalArgumentException("field quality is " + Label.QUALITY_MIN + "–" + Label.QUALITY_MAX + ", not " + d.intValue());
        return d.intValue();
    }

    private static <T> T fail(String message) {
        throw new IllegalArgumentException(message);
    }
}
