import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Holds TODO.md and docs/DONE.md (E-006): items in numeric order under their phase,
 * each with a check, the first open item's check a real one, and no number both open and done.
 * An item too large for one go is sliced into parts, 2.2a, 2.2b, ...: a part follows its step and its letter.
 */
final class TodoTest {
    private static final Pattern PHASE = Pattern.compile("(?m)^## Phase (\\d+)");
    private static final Pattern ITEM = Pattern.compile("(?m)^### (\\d+)\\.(\\d+)([a-z]?) ");
    private static final String NO_CHECK = "none in the roadmap";

    record Item(int phase, int step, String part, String body) {
        String number() {
            return phase + "." + step + part;
        }

        String check() {
            int at = body.indexOf("\nCheck: ");
            return at < 0 ? "" : body.substring(at + 8);
        }
    }

    public static void main(String[] args) {
        run().exit();
    }

    static Check run() {
        return new Check("todo")
                .repository(check(Check.read("TODO.md"), Check.read("docs/DONE.md")))
                .rejects("items out of order", () -> check(OUT_OF_ORDER, ""))
                .rejects("a first open item with no check", () -> check(FIRST_UNCHECKED, ""))
                .rejects("an item both open and done", () -> check(SOUND, "### 0.3 The container\n"))
                .rejects("parts out of order", () -> check(PARTS_OUT_OF_ORDER, ""))
                .rejects("a part with no check", () -> check(PART_UNCHECKED, ""))
                .accepts("a sound list", () -> check(SOUND, "### 0.1 Repo\n"))
                .accepts("a sliced item", () -> check(PARTS, "### 0.2 Other\n"));
    }

    /** What TODO.md gets wrong: items out of order, an item with no Check line, a first open item whose check is not written, or one also in DONE.md. */
    static List<String> check(String todo, String done) {
        List<String> failures = new ArrayList<>();
        List<Item> items = items(todo, failures);
        for (int i = 1; i < items.size(); i++) {
            Item a = items.get(i - 1), b = items.get(i);
            if (a.phase() > b.phase() || (a.phase() == b.phase() && (a.step() > b.step() || (a.step() == b.step() && a.part().compareTo(b.part()) >= 0)))) {
                failures.add(b.number() + " comes after " + a.number());
            }
        }
        for (Item item : items) {
            if (item.check().isBlank()) failures.add(item.number() + " has no Check line");
        }
        if (!items.isEmpty() && items.get(0).check().contains(NO_CHECK)) {
            failures.add("the first open item, " + items.get(0).number() + ", has no check written yet");
        }
        Set<String> open = new TreeSet<>();
        items.forEach(item -> open.add(item.number()));
        Matcher doneItem = ITEM.matcher(done);
        while (doneItem.find()) {
            String number = doneItem.group(1) + "." + doneItem.group(2) + doneItem.group(3);
            if (open.contains(number)) failures.add(number + " is both in TODO.md and in docs/DONE.md");
        }
        return failures;
    }

    /** The items in file order, each with its body; an item outside its phase's heading is a failure. */
    private static List<Item> items(String todo, List<String> failures) {
        List<Item> items = new ArrayList<>();
        Matcher item = ITEM.matcher(todo);
        List<Integer> starts = new ArrayList<>();
        while (item.find()) starts.add(item.start());
        for (int i = 0; i < starts.size(); i++) {
            int end = i + 1 < starts.size() ? starts.get(i + 1) : todo.length();
            String body = todo.substring(starts.get(i), end);
            Matcher m = ITEM.matcher(body);
            m.find();
            Item parsed = new Item(Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)), m.group(3), body);
            int heading = phaseAbove(todo, starts.get(i));
            if (heading != parsed.phase()) failures.add(parsed.number() + " sits under Phase " + heading);
            items.add(parsed);
        }
        return items;
    }

    private static int phaseAbove(String todo, int at) {
        Matcher phase = PHASE.matcher(todo.substring(0, at));
        int last = -1;
        while (phase.find()) last = Integer.parseInt(phase.group(1));
        return last;
    }

    private static final String SOUND = """
            ## Phase 0
            ### 0.3 The container
            Check: a clean clone builds offline.
            ### 0.6 Scripts
            Check: none in the roadmap; written here before the item starts.
            ## Phase 1
            ### 1.1 Ingestion
            Check: the inventory of a fixture repo is byte-identical to its golden.
            """;
    private static final String PARTS = SOUND.replace("### 0.6 Scripts\nCheck: none in the roadmap; written here before the item starts.",
            "### 0.6a Scripts\nCheck: the first part.\n### 0.6b More scripts\nCheck: the second part.\n### 0.7 After\nCheck: later.");
    private static final String PARTS_OUT_OF_ORDER = PARTS.replace("0.6b More", "0.6a More");
    private static final String PART_UNCHECKED = PARTS.replace("Check: the second part.", "");
    private static final String OUT_OF_ORDER = SOUND.replace("0.6 Scripts", "0.2 Scripts");
    private static final String FIRST_UNCHECKED = SOUND.replace("Check: a clean clone builds offline.",
            "Check: none in the roadmap; written here before the item starts.");
}
