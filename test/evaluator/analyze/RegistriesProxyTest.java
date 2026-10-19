package evaluator.analyze;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** The proxy on this machine: a local "registry" that echoes, an allow list naming it, and every refusal. */
class RegistriesProxyTest {
    private ServerSocket registry, proxy;

    /** A local registry that echoes five bytes, and the proxy in front of it, allowing it by name on its port. */
    @BeforeEach
    void start() throws IOException {
        registry = new ServerSocket(0, 50, InetAddress.getLoopbackAddress());
        Thread.ofVirtual().start(() -> {
            while (!registry.isClosed()) {
                try (Socket s = registry.accept()) {
                    s.getOutputStream().write(s.getInputStream().readNBytes(5));
                } catch (IOException e) {
                    return;
                }
            }
        });
        proxy = new ServerSocket(0, 50, InetAddress.getLoopbackAddress());
        RegistriesProxy server = new RegistriesProxy(Set.of("localhost"), registry.getLocalPort());
        Thread.ofVirtual().start(() -> server.serve(proxy));
    }

    @AfterEach
    void stop() throws IOException {
        proxy.close();
        registry.close();
    }

    private String ask(String request, String after) throws IOException {
        try (Socket s = new Socket(InetAddress.getLoopbackAddress(), proxy.getLocalPort())) {
            s.setSoTimeout(5000);
            s.getOutputStream().write(request.getBytes(StandardCharsets.ISO_8859_1));
            String status = new String(RegistriesProxy.header(s.getInputStream()).getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.ISO_8859_1).split("\r\n")[0];
            if (after == null) return status;
            s.getOutputStream().write(after.getBytes(StandardCharsets.ISO_8859_1));
            InputStream in = s.getInputStream();
            return status + " | " + new String(in.readNBytes(after.length()), StandardCharsets.ISO_8859_1);
        }
    }

    @Test
    void aRegistryOnItsPortIsTunnelled() throws IOException {
        assertEquals("HTTP/1.1 200 Connection established | hello", ask("CONNECT localhost:" + registry.getLocalPort() + " HTTP/1.1\r\nHost: x\r\n\r\n", "hello"));
    }

    @Test
    void everythingElseIsRefused() throws IOException {
        int port = registry.getLocalPort();
        assertEquals("HTTP/1.1 403 Not a registry", ask("CONNECT evil.example:" + port + " HTTP/1.1\r\n\r\n", null));
        assertEquals("HTTP/1.1 403 Not a registry", ask("CONNECT 127.0.0.1:" + port + " HTTP/1.1\r\n\r\n", null));
        assertEquals("HTTP/1.1 403 Only port " + port, ask("CONNECT localhost:22 HTTP/1.1\r\n\r\n", null));
        assertEquals("HTTP/1.1 405 Only CONNECT", ask("GET http://localhost/ HTTP/1.1\r\n\r\n", null));
        assertEquals("HTTP/1.1 400 Bad request", ask("CONNECT " + "a".repeat(RegistriesProxy.MAX_HEADER) + "\r\n\r\n", null));
    }

    @Test
    void theRealListIsTheRegistriesOnly() {
        RegistriesProxy real = new RegistriesProxy(RegistriesProxy.REGISTRIES, 443);
        assertNull(real.verdict("CONNECT registry.npmjs.org:443 HTTP/1.1\r\n\r\n"));
        assertNull(real.verdict("CONNECT Files.PythonHosted.org:443 HTTP/1.1\r\n\r\n"));
        assertEquals("403 Not a registry", real.verdict("CONNECT github.com:443 HTTP/1.1\r\n\r\n"));
        assertEquals("403 Not a registry", real.verdict("CONNECT registry.npmjs.org.evil.example:443 HTTP/1.1\r\n\r\n"));
    }
}
