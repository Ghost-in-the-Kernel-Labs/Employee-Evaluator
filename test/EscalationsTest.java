import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Holds docs/ESCALATIONS.md (E-064): each row's type one of the six, and three rows of one type not yet
 * reviewed raised to Ghost, every one marked `raised` and the type named in docs/FOR-GHOST.md. A `conflict`
 * between agents is raised at once, the first row as the third (E-066).
 */
// Decision: E-064, E-066
final class EscalationsTest {
    static final Set<String> TYPES = Set.of("missing-step", "not-reached", "wrong-step", "hard-problem", "no-replay", "conflict");
    /** Rows of one type, not yet reviewed, that go to Ghost. */
    static final int RAISE_AT = 3;
    private static final String HEADER = "| Date | Task | Route | Type | What was missing or wrong | Raised |\n| --- | --- | --- | --- | --- | --- |\n";

    public static void main(String[] args) {
        run().exit();
    }

    static Check run() {
        String row = "| 2026-10-23 | a task | `docs/how/add-a-fact.md` | missing-step | step 3 | %s |\n";
        String three = HEADER + row.formatted("") + row.formatted("") + row.formatted("");
        String raised = HEADER + row.formatted("raised") + row.formatted("raised") + row.formatted("raised");
        return new Check("escalations")
                .repository(check(Check.read("docs/ESCALATIONS.md"), Check.read("docs/FOR-GHOST.md")))
                .rejects("a type not in the list", () -> check(HEADER + "| 2026-10-23 | t | r | confusing | x |  |\n", ""))
                .rejects("a third row of a type not raised", () -> check(three, ""))
                .rejects("three raised but not in docs/FOR-GHOST.md", () -> check(raised, "- nothing about it"))
                .accepts("three raised and named for Ghost", () -> check(raised, "- **Escalations: missing-step**, three rows"))
                .accepts("three reviewed by Ghost, and a fourth new", () -> check(HEADER + row.formatted("reviewed: 2026-10-24").repeat(3) + row.formatted(""), ""))
                .accepts("two of a type, not yet raised", () -> check(HEADER + row.formatted("") + row.formatted(""), ""))
                .rejects("one conflict between agents, not raised", () -> check(HEADER + "| 2026-10-23 | t | r | conflict | two objectives |  |\n", ""))
                .accepts("one conflict raised and named for Ghost", () -> check(HEADER + "| 2026-10-23 | t | r | conflict | two objectives | raised |\n", "- **Escalations: conflict**, one row"));
    }

    static List<String> check(String log, String forGhost) {
        List<String> failures = new ArrayList<>();
        Map<String, List<String>> open = new LinkedHashMap<>();
        for (String line : log.lines().filter(l -> l.startsWith("| 2")).toList()) {
            String[] cells = line.split("\\|", -1);
            if (cells.length < 8) {
                failures.add("a row of docs/ESCALATIONS.md does not have six cells: " + line);
                continue;
            }
            String type = cells[4].trim().replace("`", ""), raised = cells[6].trim();
            if (!TYPES.contains(type)) failures.add("docs/ESCALATIONS.md: `" + type + "` is not a type; one of " + TYPES);
            else if (!raised.startsWith("reviewed")) open.computeIfAbsent(type, t -> new ArrayList<>()).add(raised);
        }
        open.forEach((type, marks) -> {
            if (marks.size() < (type.equals("conflict") ? 1 : RAISE_AT)) return;
            if (marks.stream().anyMatch(m -> !m.equals("raised"))) {
                failures.add(marks.size() + " escalations of type " + type + " not yet reviewed: mark them `raised`, name the type in docs/FOR-GHOST.md and tell Ghost (docs/how/escalate-to-opus.md)");
            } else if (!forGhost.contains(type)) {
                failures.add("escalations of type " + type + " are raised but docs/FOR-GHOST.md does not name it");
            }
        });
        return failures;
    }
}
