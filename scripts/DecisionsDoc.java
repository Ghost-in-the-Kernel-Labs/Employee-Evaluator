import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Writes docs/DECISIONS.md from theGhost.md: one line per decision, its number, title, status and subject,
 * so an agent finds the one it needs without reading the record (the Fable advisor's pass, J-062). The
 * record stays theGhost.md; this file is never edited by hand. `gradle decisionsDoc` writes it;
 * test/GhostTest.java fails when it is stale.
 */
final class DecisionsDoc {
    static final String PATH = "docs/DECISIONS.md";
    static final String SOURCE = "theGhost.md";
    private static final Pattern ENTRY = Pattern.compile("(?m)^### (E-\\d{3}): (.+)$");

    public static void main(String[] args) throws IOException {
        Files.writeString(Repo.ROOT.resolve(PATH), render(Repo.read(SOURCE)));
    }

    /** The index for this record: every `### E-nnn` entry, in order. */
    static String render(String ghost) {
        StringBuilder out = new StringBuilder("""
                # Decisions: one line each

                <!-- Written by `gradle decisionsDoc` from theGhost.md; edit theGhost.md, never this file. -->

                Find the decision here, then read its entry in `theGhost.md` by number
                (`grep -n "### E-042" theGhost.md`). The record, its rules and every reason are there.

                | Decision | Title | Status | Subject |
                | --- | --- | --- | --- |
                """);
        Matcher m = ENTRY.matcher(ghost);
        while (m.find()) {
            int end = ghost.indexOf("\n### ", m.end());
            String body = ghost.substring(m.end(), end < 0 ? ghost.length() : end);
            out.append("| ").append(m.group(1)).append(" | ").append(m.group(2).replace("|", "\\|")).append(" | ")
                    .append(field(body, "Status")).append(" | ").append(field(body, "subject")).append(" |\n");
        }
        return out.toString();
    }

    private static String field(String body, String name) {
        Matcher f = Pattern.compile("(?m)^- \\*\\*" + name + ":\\*\\* (.+)$").matcher(body);
        return f.find() ? f.group(1).replace("|", "\\|").trim() : "";
    }

    /** Whether docs/DECISIONS.md is what its generator would write. */
    static List<String> check(String doc, String ghost) {
        return doc.equals(render(ghost)) ? List.of() : List.of(PATH + " is stale or edited by hand: run `gradle decisionsDoc` and commit it");
    }
}
