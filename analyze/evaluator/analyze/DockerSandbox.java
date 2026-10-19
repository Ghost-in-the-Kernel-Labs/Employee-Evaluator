package evaluator.analyze;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * The sandbox as one throwaway Docker container per run (E-037): no credentials, a read-only root, every
 * capability dropped, no new privileges, an unprivileged user (the working copy's owner, so what the
 * run writes can be cleaned up; never root), and limits on memory, processes and CPU. In
 * TEST the container has no network; in INSTALL it is on the registries network, whose only way out is
 * the proxy that lets the package registries through (the sandbox image, TODO.md 1.5 S3).
 */
// Decision: E-037
public final class DockerSandbox implements Sandbox {
    /** The sandbox image, pinned by digest where it is built (S3). */
    public static final String IMAGE = "evaluator-sandbox";
    /** The Docker network for INSTALL: internal, its one route out the registries proxy. */
    public static final String REGISTRIES = "evaluator-registries";
    /** The registries proxy, as the install phase's tools are told to reach it. */
    public static final String PROXY = "http://registries-proxy:3128";
    /** The most of a run's output kept: its end, where the errors are. */
    static final int OUTPUT_TAIL = 64 * 1024;

    /** The `docker run` for one script: every isolation flag here, so a test holds them all. */
    static List<String> argv(Phase phase, Path work, Path out, String script, String name, String user) {
        List<String> argv = new ArrayList<>(List.of("docker", "run", "--rm", "--name", name,
                "--read-only", "--tmpfs", "/tmp:rw,nosuid,size=512m", "--cap-drop", "ALL", "--security-opt", "no-new-privileges",
                "--user", user, "--memory", "2g", "--memory-swap", "2g", "--pids-limit", "256", "--cpus", "2",
                "--mount", "type=bind,source=" + work.toAbsolutePath() + ",target=/work",
                "--mount", "type=bind,source=" + out.toAbsolutePath() + ",target=/out",
                "--workdir", "/work", "--env", "HOME=/tmp", "--env", "CI=true"));
        if (phase == Phase.TEST) {
            argv.addAll(List.of("--network", "none"));
        } else {
            argv.addAll(List.of("--network", REGISTRIES, "--env", "npm_config_ignore_scripts=true"));
            for (String variable : List.of("HTTP_PROXY", "HTTPS_PROXY", "http_proxy", "https_proxy")) argv.addAll(List.of("--env", variable + "=" + PROXY));
            argv.addAll(List.of("--env", "MAVEN_OPTS=-Dhttp.proxyHost=registries-proxy -Dhttp.proxyPort=3128 -Dhttps.proxyHost=registries-proxy -Dhttps.proxyPort=3128"));
        }
        argv.addAll(List.of(IMAGE, "sh", "-c", script));
        return argv;
    }

    /** Runs the container, its output to a file so a flood cannot fill memory; past the timeout it is killed. */
    @Override
    public Run run(Phase phase, Path work, Path out, String script, Duration timeout) {
        String name = "evaluator-" + UUID.randomUUID();
        try {
            Path log = Files.createTempFile("evaluator-run", ".log");
            Process docker = new ProcessBuilder(argv(phase, work, out, script, name, owner(work))).redirectErrorStream(true).redirectOutput(log.toFile()).start();
            boolean finished = docker.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
            if (!finished) {
                new ProcessBuilder("docker", "kill", name).redirectErrorStream(true).redirectOutput(ProcessBuilder.Redirect.DISCARD).start().waitFor(30, TimeUnit.SECONDS);
                docker.destroyForcibly();
            }
            String output = tail(log);
            Files.deleteIfExists(log);
            return new Run(finished ? docker.exitValue() : -1, !finished, output);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new Run(-1, true, "interrupted");
        }
    }

    /** The user that owns the working copy, as `uid:gid`; nobody's where the filesystem cannot say; never root. */
    static String owner(Path work) {
        try {
            int uid = (Integer) Files.getAttribute(work, "unix:uid"), gid = (Integer) Files.getAttribute(work, "unix:gid");
            return uid == 0 ? "65534:65534" : uid + ":" + gid;
        } catch (UnsupportedOperationException | IllegalArgumentException | IOException e) {
            return "65534:65534";
        }
    }

    /** The log's last OUTPUT_TAIL bytes, read from its end: a run that printed gigabytes costs this much memory. */
    // Unsure: the log itself can still grow on the host's disk until the timeout; a cap belongs with the fleet (TODO.md 2.7).
    static String tail(Path log) throws IOException {
        try (java.io.RandomAccessFile file = new java.io.RandomAccessFile(log.toFile(), "r")) {
            long from = Math.max(0, file.length() - OUTPUT_TAIL);
            byte[] bytes = new byte[(int) (file.length() - from)];
            file.seek(from);
            file.readFully(bytes);
            return new String(bytes, StandardCharsets.UTF_8);
        }
    }
}
