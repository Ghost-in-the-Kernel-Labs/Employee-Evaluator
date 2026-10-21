package evaluator.github;

import java.io.IOException;

/**
 * GitHub's REST API, read only: one GET of a path under `https://api.github.com`, its JSON parsed. The live
 * adapter is `HttpGitHub`; a test stands recorded answers in for it.
 */
public interface GitHubApi {
    /** The parsed JSON GitHub answers for this path (`/repos/o/r/commits?page=1`); an IOException for anything but 200. */
    Object get(String path) throws IOException;
}
