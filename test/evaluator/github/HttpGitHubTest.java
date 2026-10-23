package evaluator.github;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The live adapter answered from loopback: a recorded answer reaches every line of `get`, with no call to
 * GitHub (the host is the test's own server, through the package-private `HttpGitHub.at`). The token here is
 * a made-up string, not the environment's.
 */
class HttpGitHubTest {
    private HttpServer server;
    private String host;
    private final AtomicReference<Map<String, List<String>>> seen = new AtomicReference<>();

    /** A loopback server with one path per answer HttpGitHub must handle: ok, 404, a redirect, HTML, and a body at and past the cap. */
    @BeforeEach
    void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/ok", ex -> {
            seen.set(Map.copyOf(ex.getRequestHeaders()));
            respond(ex, 200, "[{\"sha\": \"abc\"}]".getBytes(StandardCharsets.UTF_8));
        });
        server.createContext("/missing", ex -> respond(ex, 404, "{\"message\": \"Not Found\"}".getBytes(StandardCharsets.UTF_8)));
        server.createContext("/moved", ex -> {
            ex.getResponseHeaders().add("Location", "/ok");
            respond(ex, 302, new byte[0]);
        });
        server.createContext("/html", ex -> respond(ex, 200, "<html>".getBytes(StandardCharsets.UTF_8)));
        server.createContext("/exact", ex -> respond(ex, 200, padded(HttpGitHub.MAX_BODY)));
        server.createContext("/over", ex -> respond(ex, 200, padded(HttpGitHub.MAX_BODY + 1)));
        server.start();
        host = "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @AfterEach
    void stop() {
        server.stop(0);
    }

    private static void respond(com.sun.net.httpserver.HttpExchange ex, int status, byte[] body) throws IOException {
        ex.sendResponseHeaders(status, body.length == 0 ? -1 : body.length);
        if (body.length > 0) ex.getResponseBody().write(body);
        ex.close();
    }

    /** "[]" with spaces between, in all `size` bytes: valid JSON of exactly that length. */
    private static byte[] padded(int size) {
        byte[] bytes = new byte[size];
        Arrays.fill(bytes, (byte) ' ');
        bytes[0] = '[';
        bytes[size - 1] = ']';
        return bytes;
    }

    @Test
    void anAnswerIsParsedAndAskedForWithTheStrippedTokenAndNothingElseSecret() throws IOException {
        Object answer = HttpGitHub.at(host, "tok-123").get("/ok");
        assertEquals(List.of(Map.of("sha", "abc")), answer);
        Map<String, List<String>> headers = seen.get();
        assertEquals(List.of("Bearer tok-123"), headers.get("Authorization"));
        assertEquals(List.of("application/vnd.github+json"), headers.get("Accept"));
        assertEquals(List.of("2022-11-28"), headers.get("X-github-api-version"));
    }

    @Test
    void theEnvironmentsTokenIsStrippedAndAMissingOrBlankOneRefused() throws IOException {
        for (String none : new String[] {null, "", "  \n"}) {
            IOException refused = assertThrows(IOException.class, () -> HttpGitHub.from(none));
            assertEquals("EVALUATOR_GITHUB_TOKEN is not set", refused.getMessage());
        }
        HttpGitHub live = HttpGitHub.from(" tok-9 \n");
        assertEquals("HttpGitHub(https://api.github.com)", live.toString());
        assertFalse(live.toString().contains("tok-9"));
    }

    @Test
    void aPathThatIsNotAnApiPathIsRefusedBeforeAnythingIsSent() {
        HttpGitHub api = HttpGitHub.at(host, "t");
        for (String bad : List.of("ok", "/a/../ok", "/ok?next=http://evil.example/x", "https://evil.example/ok")) {
            IOException refused = assertThrows(IOException.class, () -> api.get(bad), bad);
            assertEquals("not an API path: " + bad, refused.getMessage());
        }
        assertEquals(null, seen.get(), "nothing reached the server");
    }

    @Test
    void aCompareRangeOfThreeDotsIsAnApiPathButADotDotSegmentIsNot() throws IOException {
        HttpGitHub api = HttpGitHub.at(host, "t");
        for (String bad : List.of("/a/..", "/../ok", "/a/../ok")) {
            assertThrows(IOException.class, () -> api.get(bad), bad);
        }
        assertEquals(null, seen.get(), "nothing reached the server");
        try {
            api.get("/repos/o/r/compare/abc...def");
        } catch (IOException e) {
            assertFalse(e.getMessage().startsWith("not an API path"), e.getMessage());
        }
    }

    @Test
    void anythingButA200IsRefusedWithItsStatusAndARedirectIsNotFollowed() {
        HttpGitHub api = HttpGitHub.at(host, "t");
        assertEquals("GitHub answered 404 for /missing", assertThrows(IOException.class, () -> api.get("/missing")).getMessage());
        assertEquals("GitHub answered 302 for /moved", assertThrows(IOException.class, () -> api.get("/moved")).getMessage());
        assertEquals(null, seen.get(), "the redirect's target was never asked for");
    }

    @Test
    void anAnswerThatIsNotJsonIsSaidSo() {
        assertEquals("GitHub's answer for /html is not JSON", assertThrows(IOException.class, () -> HttpGitHub.at(host, "t").get("/html")).getMessage());
    }

    @Test
    void theAnswerIsCappedAtExactlyTheMostAndOneByteMoreIsRefused() throws IOException {
        HttpGitHub api = HttpGitHub.at(host, "t");
        assertEquals(List.of(), api.get("/exact"));
        IOException over = assertThrows(IOException.class, () -> api.get("/over"));
        assertEquals("GitHub's answer for /over is over " + HttpGitHub.MAX_BODY + " bytes", over.getMessage());
    }

    @Test
    void anInterruptedCallSaysSoAndKeepsTheInterrupt() {
        Thread.currentThread().interrupt();
        try {
            IOException e = assertThrows(IOException.class, () -> HttpGitHub.at(host, "t").get("/ok"));
            assertEquals("interrupted", e.getMessage());
            assertTrue(Thread.currentThread().isInterrupted(), "the flag is set again for whoever called");
        } finally {
            Thread.interrupted();
        }
    }
}
