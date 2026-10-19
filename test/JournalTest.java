import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Holds docs/JOURNAL.md (E-008): every entry's id is greater than every id before it, and every entry
 * says who was there and its class. KNOWN lists exceptions already in the file, each once, none allowed again.
 */
// Decision: E-008
final class JournalTest {
    private static final Pattern ENTRY = Pattern.compile("(?m)^### J-(\\d{3})");
    private static final Set<String> KNOWN = Set.of();

    public static void main(String[] args) {
        run().exit();
    }

    static Check run() {
        String entry = "### J-%s A title\n- **Who:** Opus. **Class:** build.\n- **Found:** x\n\n";
        return new Check("journal")
                .repository(check(Check.read("docs/JOURNAL.md")))
                .rejects("an id used twice", () -> check(entry.formatted("001") + entry.formatted("001")))
                .rejects("an id out of order", () -> check(entry.formatted("002") + entry.formatted("001")))
                .rejects("an entry with no class", () -> check(entry.formatted("001").replace(" **Class:** build.", "")))
                .accepts("two entries in order", () -> check(entry.formatted("001") + entry.formatted("002")));
    }

    /** Each entry, in order: its id above every earlier one (but the known exception), its Who and Class. */
    static List<String> check(String journal) {
        List<String> failures = new ArrayList<>();
        Matcher m = ENTRY.matcher(journal);
        List<int[]> entries = new ArrayList<>();
        while (m.find()) entries.add(new int[] {Integer.parseInt(m.group(1)), m.start()});
        int highest = 0;
        for (int i = 0; i < entries.size(); i++) {
            int id = entries.get(i)[0];
            String body = journal.substring(entries.get(i)[1], i + 1 < entries.size() ? entries.get(i + 1)[1] : journal.length());
            String name = "J-%03d".formatted(id);
            if (id <= highest && !KNOWN.contains(name + " after J-%03d".formatted(highest))) failures.add(name + " comes after J-%03d: an id is new and above every one before it".formatted(highest));
            highest = Math.max(highest, id);
            if (!body.contains("**Who:**") || !body.contains("**Class:**")) failures.add(name + " does not say who was there and its class (E-008)");
        }
        return failures;
    }
}
