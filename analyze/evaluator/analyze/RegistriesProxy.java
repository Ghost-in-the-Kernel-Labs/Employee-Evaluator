package evaluator.analyze;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.Semaphore;

/**
 * The install phase's one way out (E-037): an HTTPS proxy that opens a tunnel only to the package
 * registries, by exact host name, on port 443, and refuses everything else. It reads nothing inside a
 * tunnel. It runs in its own container, on the registries network and the outside one (sandbox/setup.sh).
 */
// Decision: E-037
public final class RegistriesProxy {
    /** The registries the install phase may reach: npm's, PyPI's (its index and its files), Maven Central. */
    public static final Set<String> REGISTRIES = Set.of("registry.npmjs.org", "pypi.org", "files.pythonhosted.org", "repo.maven.apache.org", "repo1.maven.org");
    static final int MAX_HEADER = 8 * 1024;
    static final int IDLE_MILLIS = 60_000;
    static final int MAX_TUNNELS = 64;

    private final Set<String> hosts;
    private final int port;
    private final Semaphore tunnels = new Semaphore(MAX_TUNNELS);

    RegistriesProxy(Set<String> hosts, int port) {
        this.hosts = hosts;
        this.port = port;
    }

    /** `java -cp ... evaluator.analyze.RegistriesProxy 3128`: serves until the container stops. */
    public static void main(String[] args) throws IOException {
        int listen = args.length > 0 ? Integer.parseInt(args[0]) : 3128;
        try (ServerSocket server = new ServerSocket()) {
            server.bind(new InetSocketAddress(listen));
            new RegistriesProxy(REGISTRIES, 443).serve(server);
        }
    }

    /** Accepts connections until the socket closes, each on its own virtual thread. */
    void serve(ServerSocket server) {
        while (!server.isClosed()) {
            try {
                Socket client = server.accept();
                Thread.ofVirtual().start(() -> handle(client));
            } catch (IOException e) {
                if (server.isClosed()) return;
            }
        }
    }

    /** One client: its request read, refused or tunnelled; closed either way. */
    void handle(Socket client) {
        try (client) {
            client.setSoTimeout(IDLE_MILLIS);
            String request = header(client.getInputStream());
            String verdict = verdict(request);
            if (verdict != null) {
                reply(client.getOutputStream(), verdict);
                return;
            }
            String[] target = request.split(" ")[1].split(":");
            if (!tunnels.tryAcquire()) {
                reply(client.getOutputStream(), "503 Too many tunnels");
                return;
            }
            try (Socket upstream = new Socket()) {
                upstream.connect(new InetSocketAddress(target[0], Integer.parseInt(target[1])), 10_000);
                upstream.setSoTimeout(IDLE_MILLIS);
                reply(client.getOutputStream(), "200 Connection established");
                Thread back = Thread.ofVirtual().start(() -> pipe(upstream, client));
                pipe(client, upstream);
                back.join();
            } finally {
                tunnels.release();
            }
        } catch (IOException | InterruptedException e) {
            // A client or a registry that went away: nothing to answer.
        }
    }

    /** Why a request is refused, as a status line; null when it is a CONNECT to an allowed registry on the allowed port. */
    String verdict(String request) {
        if (request == null) return "400 Bad request";
        String[] parts = request.split("\r\n", 2)[0].split(" ");
        if (parts.length != 3 || !parts[0].equals("CONNECT")) return "405 Only CONNECT";
        String[] target = parts[1].split(":");
        if (target.length != 2 || !target[1].equals(String.valueOf(port))) return "403 Only port " + port;
        return hosts.contains(target[0].toLowerCase(Locale.ROOT)) ? null : "403 Not a registry";
    }

    /** The request's header, up to its blank line; null when it is longer than MAX_HEADER or never ends. */
    static String header(InputStream in) throws IOException {
        ByteArrayOutputStream read = new ByteArrayOutputStream();
        while (read.size() < MAX_HEADER) {
            int b = in.read();
            if (b < 0) return null;
            read.write(b);
            if (read.toString(StandardCharsets.ISO_8859_1).endsWith("\r\n\r\n")) return read.toString(StandardCharsets.ISO_8859_1);
        }
        return null;
    }

    private static void reply(OutputStream out, String status) throws IOException {
        out.write(("HTTP/1.1 " + status + "\r\n\r\n").getBytes(StandardCharsets.ISO_8859_1));
        out.flush();
    }

    /** Copies one socket into the other until either closes or goes idle, then closes the far side's output. */
    private static void pipe(Socket from, Socket to) {
        try {
            from.getInputStream().transferTo(to.getOutputStream());
            to.shutdownOutput();
        } catch (IOException e) {
            // Either end went away or idled out; the other copy stops with it.
        }
    }
}
