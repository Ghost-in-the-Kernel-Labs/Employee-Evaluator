package evaluator.github;

import evaluator.rubric.Json;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

/**
 * GitHub's API over HTTPS (E-043): GET only, to `api.github.com` only, with a read-only token taken from
 * the environment (`EVALUATOR_GITHUB_TOKEN`) and never written anywhere: not into a message, an error or a
 * fact. The kernel's one door to the network (MOD-05).
 */
// Decision: E-043
public final class HttpGitHub implements GitHubApi {
    /** The environment variable holding the token: fine-grained, read only, with an expiry. */
    public static final String TOKEN_VARIABLE = "EVALUATOR_GITHUB_TOKEN";
    static final String HOST = "https://api.github.com";
    /** The most of an answer read; a larger one is refused. */
    static final int MAX_BODY = 10 * 1024 * 1024;

    private final String host;
    private final String token;
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).followRedirects(HttpClient.Redirect.NEVER).build();

    private HttpGitHub(String host, String token) {
        this.host = host;
        this.token = token;
    }

    /** The adapter with the environment's token; an IOException when there is none, naming the variable, never a value. */
    public static HttpGitHub fromEnvironment() throws IOException {
        return from(System.getenv(TOKEN_VARIABLE));
    }

    /** The adapter for a token as the environment gave it (null or blank: refused, as above); the token is stripped. */
    static HttpGitHub from(String token) throws IOException {
        if (token == null || token.isBlank()) throw new IOException(TOKEN_VARIABLE + " is not set");
        return new HttpGitHub(HOST, token.strip());
    }

    /** The same adapter against another host, so a test can answer it from loopback; production code never calls this. */
    static HttpGitHub at(String host, String token) {
        return new HttpGitHub(host, token);
    }

    /** One GET of an API path (never a URL of its own), its answer capped, refused unless 200, parsed. */
    @Override
    public Object get(String path) throws IOException {
        if (!path.startsWith("/") || List.of(path.split("/", -1)).contains("..") || path.contains("://")) throw new IOException("not an API path: " + path);
        HttpRequest request = HttpRequest.newBuilder(URI.create(host + path)).timeout(Duration.ofSeconds(30)).GET()
                .header("Accept", "application/vnd.github+json").header("X-GitHub-Api-Version", "2022-11-28")
                .header("Authorization", "Bearer " + token).build();
        try {
            HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
            try (InputStream body = response.body()) {
                byte[] bytes = body.readNBytes(MAX_BODY + 1);
                if (bytes.length > MAX_BODY) throw new IOException("GitHub's answer for " + path + " is over " + MAX_BODY + " bytes");
                if (response.statusCode() != 200) throw new IOException("GitHub answered " + response.statusCode() + " for " + path);
                return Json.parse(new String(bytes, StandardCharsets.UTF_8));
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("interrupted");
        } catch (IllegalArgumentException e) {
            throw new IOException("GitHub's answer for " + path + " is not JSON");
        }
    }

    /** Never the token. */
    @Override
    public String toString() {
        return "HttpGitHub(" + host + ")";
    }
}
