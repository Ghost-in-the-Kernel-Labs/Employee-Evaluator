package evaluator.github;

import evaluator.rubric.Evidence;
import evaluator.rubric.Fact;
import evaluator.rubric.Subject;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Provenance layer 2 (docs/DESIGN.md, E-043): what GitHub verifies of each commit, against the account that
 * submitted. A commit is signed (GitHub verified its signature and the applicant is its author), pushed
 * (unsigned, but GitHub's push log shows the applicant pushed it), claimed (the applicant's by its author
 * field alone, which one flag forges), foreign (another GitHub account's) or unmatched (no account's).
 * GitHub keeps 90 days of push events, at most 300: a push before them cannot be seen, and the facts say how
 * far back they reach.
 */
// Decision: E-043
public final class Authorship {
    /** The tool every authorship fact names as its evidence. */
    public static final String TOOL = "evaluator github 1 (api.github.com)";
    static final int MAX_COMMIT_PAGES = 50;
    static final int MAX_EVENT_PAGES = 3;
    private static final Pattern NAME = Pattern.compile("[A-Za-z0-9._-]{1,100}");
    private static final String NO_PARENT = "0000000000000000000000000000000000000000";

    /** The classes a commit falls into, in the order the facts are given. */
    public enum Kind { SIGNED, PUSHED, CLAIMED, FOREIGN, UNMATCHED }

    private Authorship() {}

    /** The authorship facts of owner/repo for the applicant's account; names that could steer a URL are refused. */
    public static List<Fact> facts(GitHubApi api, String owner, String repo, String applicant) throws IOException {
        for (String name : List.of(owner, repo, applicant)) if (!NAME.matcher(name).matches()) throw new IOException("not a GitHub name: " + name);
        String base = "/repos/" + owner + "/" + repo;
        List<Map<?, ?>> commits = pages(api, base + "/commits?per_page=100&page=", MAX_COMMIT_PAGES);
        Pushes pushes = pushes(api, base, applicant);
        Map<Kind, List<String>> kinds = new LinkedHashMap<>();
        for (Kind kind : Kind.values()) kinds.put(kind, new ArrayList<>());
        Instant newest = Instant.EPOCH;
        for (Map<?, ?> commit : commits) {
            kinds.get(classify(commit, applicant, pushes.shas())).add(String.valueOf(commit.get("sha")));
            Instant at = date(commit);
            if (at.isAfter(newest)) newest = at;
        }
        return facts(kinds, commits.size(), pushes, newest);
    }

    /** One commit's class. */
    static Kind classify(Map<?, ?> commit, String applicant, Set<String> pushed) {
        String author = login(commit.get("author"));
        if (author == null) return Kind.UNMATCHED;
        if (!author.equalsIgnoreCase(applicant)) return Kind.FOREIGN;
        Object verification = commit.get("commit") instanceof Map<?, ?> c ? c.get("verification") : null;
        if (verification instanceof Map<?, ?> v && Boolean.TRUE.equals(v.get("verified"))) return Kind.SIGNED;
        return pushed.contains(String.valueOf(commit.get("sha"))) ? Kind.PUSHED : Kind.CLAIMED;
    }

    /** The commits the applicant pushed in GitHub's push log, and the oldest push it holds (null with none). */
    record Pushes(Set<String> shas, Instant oldest) {}

    /**
     * The applicant's pushes: each PushEvent's commits, from its payload when it lists them, otherwise from
     * GitHub's compare of the push's before and head (a new branch's push gives its head alone).
     */
    static Pushes pushes(GitHubApi api, String base, String applicant) throws IOException {
        Set<String> shas = new HashSet<>();
        Instant oldest = null;
        for (Map<?, ?> event : pages(api, base + "/events?per_page=100&page=", MAX_EVENT_PAGES)) {
            if (!"PushEvent".equals(event.get("type")) || !applicant.equalsIgnoreCase(login(event.get("actor")))) continue;
            Instant at = Instant.parse(String.valueOf(event.get("created_at")));
            if (oldest == null || at.isBefore(oldest)) oldest = at;
            Map<?, ?> payload = event.get("payload") instanceof Map<?, ?> p ? p : Map.of();
            if (payload.get("commits") instanceof List<?> listed && !listed.isEmpty()) {
                for (Object c : listed) if (c instanceof Map<?, ?> m) shas.add(String.valueOf(m.get("sha")));
                continue;
            }
            String head = String.valueOf(payload.get("head")), before = String.valueOf(payload.get("before"));
            if (!head.matches("[0-9a-f]{40}")) continue;
            shas.add(head);
            if (before.matches("[0-9a-f]{40}") && !before.equals(NO_PARENT)) {
                Object compared = api.get(base + "/compare/" + before + "..." + head);
                if (compared instanceof Map<?, ?> m && m.get("commits") instanceof List<?> list) {
                    for (Object c : list) if (c instanceof Map<?, ?> cm) shas.add(String.valueOf(cm.get("sha")));
                }
            }
        }
        return new Pushes(shas, oldest);
    }

    /** A count per class, citing its commits; the applicant's share; how far back the push log reaches. */
    private static List<Fact> facts(Map<Kind, List<String>> kinds, int total, Pushes pushes, Instant newest) {
        List<Fact> facts = new ArrayList<>();
        if (total == 0) return facts;
        kinds.forEach((kind, shas) -> facts.add(fact(kind.name().toLowerCase(Locale.ROOT), shas.size(), shas.isEmpty() ? List.of("(none)") : shas)));
        int theirs = kinds.get(Kind.SIGNED).size() + kinds.get(Kind.PUSHED).size() + kinds.get(Kind.CLAIMED).size();
        facts.add(fact("applicant_share", Math.round(1000.0 * theirs / total) / 1000.0, List.of(theirs + " of " + total + " commits")));
        long window = pushes.oldest() == null ? 0 : Math.max(0, Duration.between(pushes.oldest(), newest).toDays());
        facts.add(fact("push_window_days", window, List.of(pushes.oldest() == null ? "(no push by the applicant in GitHub's log)" : "oldest push " + pushes.oldest())));
        return facts;
    }

    /** Every item of a paged list, page by page until one comes back empty or the cap is reached. */
    private static List<Map<?, ?>> pages(GitHubApi api, String path, int maxPages) throws IOException {
        List<Map<?, ?>> items = new ArrayList<>();
        for (int page = 1; page <= maxPages; page++) {
            if (!(api.get(path + page) instanceof List<?> list) || list.isEmpty()) break;
            for (Object item : list) if (item instanceof Map<?, ?> m) items.add(m);
        }
        return items;
    }

    private static String login(Object account) {
        return account instanceof Map<?, ?> m && m.get("login") instanceof String login ? login : null;
    }

    private static Instant date(Map<?, ?> commit) {
        try {
            Map<?, ?> c = (Map<?, ?>) commit.get("commit");
            return Instant.parse(String.valueOf(((Map<?, ?>) c.get("committer")).get("date")));
        } catch (RuntimeException e) {
            return Instant.EPOCH;
        }
    }

    private static Fact fact(String kind, Number value, List<String> sources) {
        return Fact.measured("PROVENANCE.authorship." + kind, Subject.repository(), value, new Evidence(TOOL, sources));
    }
}
