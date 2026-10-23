package evaluator.analyze;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
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
    private final List<ServerSocket> opened = new ArrayList<>();
    private final List<Socket> held = new ArrayList<>();

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
        for (ServerSocket server : opened) server.close();
        for (Socket socket : held) socket.close();
    }

    /** Another loopback listener, closed after the test. */
    private ServerSocket listener() throws IOException {
        ServerSocket server = new ServerSocket(0, 50, InetAddress.getLoopbackAddress());
        opened.add(server);
        return server;
    }

    /** A "registry" that takes connections and never reads or writes: only the proxy's own limits end a tunnel to it. */
    private int silentRegistry() throws IOException {
        ServerSocket server = listener();
        Thread.ofVirtual().start(() -> {
            while (!server.isClosed()) {
                try {
                    Socket s = server.accept();
                    synchronized (held) { held.add(s); }
                } catch (IOException e) {
                    return;
                }
            }
        });
        return server.getLocalPort();
    }

    /** A proxy with the given limits in front of `localhost` on `registryPort`; the port it listens on. */
    private int proxyWith(int registryPort, int idleMillis, int maxTunnels) throws IOException {
        ServerSocket server = listener();
        RegistriesProxy p = new RegistriesProxy(Set.of("localhost"), registryPort, idleMillis, maxTunnels);
        Thread.ofVirtual().start(() -> p.serve(server));
        return server.getLocalPort();
    }

    private Socket client(int port) throws IOException {
        Socket s = new Socket(InetAddress.getLoopbackAddress(), port);
        s.setSoTimeout(5000);
        held.add(s);
        return s;
    }

    /** The first line of the proxy's answer, or "(none)" when it closed without one. */
    private static String statusOf(Socket s) throws IOException {
        String header = RegistriesProxy.header(s.getInputStream());
        return header == null ? "(none)" : header.split("\r\n")[0];
    }

    private static void connect(Socket s, int registryPort) throws IOException {
        s.getOutputStream().write(("CONNECT localhost:" + registryPort + " HTTP/1.1\r\n\r\n").getBytes(StandardCharsets.ISO_8859_1));
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

    /**
     * The header's two edges and its two ways to end without one. MAX_HEADER = 8192 bytes: a terminator that ends at
     * byte 8192 is read; one that ends at byte 8193 is not (the 8193rd byte is never asked for). A NUL byte is a
     * byte, not the end of the stream (-1). A stream that stops before its blank line gives null.
     */
    @Test
    void theHeaderIsReadToItsBlankLineAndNoFurther() throws IOException {
        String edge = "a".repeat(RegistriesProxy.MAX_HEADER - 4) + "\r\n\r\n";
        assertEquals(edge, RegistriesProxy.header(new ByteArrayInputStream(edge.getBytes(StandardCharsets.ISO_8859_1))));
        String over = "a".repeat(RegistriesProxy.MAX_HEADER - 3) + "\r\n\r\n";
        assertNull(RegistriesProxy.header(new ByteArrayInputStream(over.getBytes(StandardCharsets.ISO_8859_1))));
        assertEquals("a\0b\r\n\r\n", RegistriesProxy.header(new ByteArrayInputStream("a\0b\r\n\r\n".getBytes(StandardCharsets.ISO_8859_1))));
        assertNull(RegistriesProxy.header(new ByteArrayInputStream("CONNECT x".getBytes(StandardCharsets.ISO_8859_1))));
    }

    /** A client that says nothing is let go at the idle limit (300 ms here): the proxy closes, which the client reads as -1. */
    @Test
    void aClientThatSaysNothingIsLetGo() throws IOException {
        try (Socket s = client(proxyWith(1, 300, 64))) {
            assertEquals(-1, s.getInputStream().read());
        }
    }

    /** A registry that goes quiet after the tunnel opens: the far side idles out, the tunnel closes, the client sees the end. */
    @Test
    void aSilentRegistryEndsItsTunnelAtTheIdleLimit() throws IOException {
        int registryPort = silentRegistry();
        try (Socket s = client(proxyWith(registryPort, 300, 64))) {
            connect(s, registryPort);
            assertEquals("HTTP/1.1 200 Connection established", statusOf(s));
            assertEquals(-1, s.getInputStream().read());
        }
    }

    /** With room for one tunnel, a second is refused with 503, and once the first has closed its place is given back. */
    @Test
    void tunnelsAreCappedAndTheirPlaceIsGivenBack() throws IOException, InterruptedException {
        int registryPort = silentRegistry();
        int port = proxyWith(registryPort, 500, 1);
        Socket first = client(port);
        connect(first, registryPort);
        assertEquals("HTTP/1.1 200 Connection established", statusOf(first));
        Socket second = client(port);
        connect(second, registryPort);
        assertEquals("HTTP/1.1 503 Too many tunnels", statusOf(second));
        first.close();
        String again = "";
        long deadline = System.nanoTime() + 5_000_000_000L;
        while (!again.equals("HTTP/1.1 200 Connection established") && System.nanoTime() < deadline) {
            Socket third = client(port);
            connect(third, registryPort);
            again = statusOf(third);
            third.close();
            Thread.sleep(50);
        }
        assertEquals("HTTP/1.1 200 Connection established", again);
    }

    /**
     * A client that has sent everything and half-closes still gets the answer: the registry here replies "done" only
     * after it has read to the end of what it was sent, so the proxy must pass the client's end on (shutdownOutput)
     * and wait for the registry's side (join) before it closes the client.
     */
    @Test
    void aHalfClosedClientStillGetsTheAnswer() throws IOException {
        ServerSocket waits = listener();
        Thread.ofVirtual().start(() -> {
            try (Socket s = waits.accept()) {
                s.getInputStream().readAllBytes();
                s.getOutputStream().write("done".getBytes(StandardCharsets.ISO_8859_1));
            } catch (IOException e) {
                // The test failed elsewhere.
            }
        });
        try (Socket s = client(proxyWith(waits.getLocalPort(), 60_000, 64))) {
            connect(s, waits.getLocalPort());
            assertEquals("HTTP/1.1 200 Connection established", statusOf(s));
            s.getOutputStream().write('x');
            s.shutdownOutput();
            assertEquals("done", new String(s.getInputStream().readAllBytes(), StandardCharsets.ISO_8859_1));
        }
    }

    /** An accept that fails once with the socket still open (a descriptor shortage, say) does not end the proxy. */
    @Test
    void anAcceptThatFailsOnceDoesNotStopTheProxy() throws IOException {
        AtomicInteger accepts = new AtomicInteger();
        ServerSocket flaky = new ServerSocket(0, 50, InetAddress.getLoopbackAddress()) {
            @Override
            public Socket accept() throws IOException {
                if (accepts.getAndIncrement() == 0) throw new IOException("a transient failure");
                return super.accept();
            }
        };
        opened.add(flaky);
        RegistriesProxy p = new RegistriesProxy(Set.of("localhost"), 1);
        Thread.ofVirtual().start(() -> p.serve(flaky));
        try (Socket s = client(flaky.getLocalPort())) {
            s.getOutputStream().write("GET / HTTP/1.1\r\n\r\n".getBytes(StandardCharsets.ISO_8859_1));
            assertEquals("HTTP/1.1 405 Only CONNECT", statusOf(s));
        }
        assertTrue(accepts.get() >= 2);
    }

    /** The port is the first argument, else 3128. */
    @Test
    void theListeningPortIsTheArgumentOrThe3128() {
        assertEquals(3128, RegistriesProxy.listenPort(new String[0]));
        assertEquals(4000, RegistriesProxy.listenPort(new String[] {"4000"}));
    }

    /**
     * `main` binds the port it is given and serves the real list on 443. Only refusals are asked of it (a host that is not
     * a registry; a registry on port 80), so nothing dials out. It serves until the JVM ends: its thread is virtual, so
     * it does not hold the test run open.
     */
    @Test
    void mainBindsItsPortAndServesTheRealList() throws IOException, InterruptedException {
        int port;
        try (ServerSocket free = new ServerSocket(0)) {
            port = free.getLocalPort();
        }
        Thread.ofVirtual().start(() -> {
            try {
                RegistriesProxy.main(new String[] {String.valueOf(port)});
            } catch (IOException e) {
                // The test would fail to connect.
            }
        });
        Socket s = null;
        long deadline = System.nanoTime() + 5_000_000_000L;
        while (s == null) {
            try {
                s = client(port);
            } catch (IOException e) {
                if (System.nanoTime() > deadline) throw e;
                Thread.sleep(50);
            }
        }
        s.getOutputStream().write("CONNECT evil.example:443 HTTP/1.1\r\n\r\n".getBytes(StandardCharsets.ISO_8859_1));
        assertEquals("HTTP/1.1 403 Not a registry", statusOf(s));
        Socket other = client(port);
        other.getOutputStream().write("CONNECT registry.npmjs.org:80 HTTP/1.1\r\n\r\n".getBytes(StandardCharsets.ISO_8859_1));
        assertEquals("HTTP/1.1 403 Only port 443", statusOf(other));
    }
}
