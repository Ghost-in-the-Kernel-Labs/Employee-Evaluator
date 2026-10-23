package evaluator.analyze;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** The exact `docker run` for each phase: every isolation flag present, nothing that would open a door. */
class DockerSandboxTest {
    private static final Path WORK = Path.of("/tmp/w"), OUT = Path.of("/tmp/o");

    @Test
    void theTestPhaseHasNoNetworkAndEveryLimit() {
        List<String> argv = DockerSandbox.argv(Sandbox.Phase.TEST, WORK, OUT, "pytest", "evaluator-1", "1000:1000");
        assertEquals(List.of("docker", "run", "--rm", "--name", "evaluator-1", "--read-only", "--tmpfs", "/tmp:rw,nosuid,size=512m",
                "--cap-drop", "ALL", "--security-opt", "no-new-privileges", "--user", "1000:1000", "--memory", "2g", "--memory-swap", "2g",
                "--pids-limit", "256", "--cpus", "2", "--mount", "type=bind,source=/tmp/w,target=/work", "--mount", "type=bind,source=/tmp/o,target=/out",
                "--workdir", "/work", "--env", "HOME=/tmp", "--env", "CI=true", "--network", "none", "evaluator-sandbox", "sh", "-c", "pytest"), argv);
    }

    @Test
    void theInstallPhaseReachesOnlyTheRegistriesWithScriptsOff() {
        List<String> argv = DockerSandbox.argv(Sandbox.Phase.INSTALL, WORK, OUT, "npm ci", "evaluator-2", "1000:1000");
        int network = argv.indexOf("--network");
        assertEquals("evaluator-registries", argv.get(network + 1));
        assertTrue(argv.contains("npm_config_ignore_scripts=true"));
        assertTrue(argv.contains("HTTPS_PROXY=http://registries-proxy:3128"));
    }

    @Test
    void noPhaseOpensADoor() {
        for (Sandbox.Phase phase : Sandbox.Phase.values()) {
            List<String> argv = DockerSandbox.argv(phase, WORK, OUT, "x", "n", "1000:1000");
            for (String flag : List.of("--privileged", "--pid", "--ipc", "--uts", "--cap-add", "-v", "--volume", "--device", "--userns")) {
                assertFalse(argv.stream().anyMatch(a -> a.equals(flag) || a.startsWith(flag + "=")), phase + " passes " + flag);
            }
            String joined = String.join(" ", argv);
            for (String door : List.of("docker.sock", "--network host", "TOKEN", "SECRET", "PASSWORD")) assertFalse(joined.contains(door), phase + " opens " + door);
        }
    }

    @Test
    void neverRoot(@TempDir Path dir) {
        String owner = DockerSandbox.owner(dir);
        assertFalse(owner.startsWith("0:"), owner);
    }

    /** As root (the scorer on a server): the tree goes to nobody, and a link planted in it moves, never its target. */
    @Test
    void aTreeIsHandedToNobodyAndALinkIsNotFollowed(@TempDir Path work, @TempDir Path outside) throws IOException {
        org.junit.jupiter.api.Assumptions.assumeTrue(((Integer) Files.getAttribute(work, "unix:uid")) == 0, "only root can hand a tree over");
        Files.writeString(Files.createDirectories(work.resolve("app")).resolve("a.py"), "x = 1\n");
        Path secret = Files.writeString(outside.resolve("secret"), "the host's");
        Files.createSymbolicLink(work.resolve("app/link"), secret);
        DockerSandbox.handOver(work);
        assertEquals(65534, Files.getAttribute(work.resolve("app/a.py"), "unix:uid"));
        assertEquals(65534, Files.getAttribute(work.resolve("app"), "unix:gid"));
        assertEquals(65534, Files.getAttribute(work.resolve("app/link"), "unix:uid", java.nio.file.LinkOption.NOFOLLOW_LINKS));
        assertEquals(0, Files.getAttribute(secret, "unix:uid"), "the link's target is the host's and stays so");
        assertEquals(DockerSandbox.NOBODY, DockerSandbox.owner(work));
    }

    @Test
    void onlyTheEndOfAFloodIsKept(@TempDir Path dir) throws IOException {
        Path log = dir.resolve("run.log");
        Files.writeString(log, "x".repeat(5 * DockerSandbox.OUTPUT_TAIL) + "the error");
        String tail = DockerSandbox.tail(log);
        assertEquals(DockerSandbox.OUTPUT_TAIL, tail.length());
        assertTrue(tail.endsWith("the error"));
    }

    /** A script standing in for `docker`: no Docker is started. */
    private static String fakeDocker(Path dir, String body) throws IOException {
        Path script = Files.writeString(dir.resolve("docker"), "#!/bin/sh\n" + body);
        assertTrue(script.toFile().setExecutable(true));
        return script.toString();
    }

    private static List<String> lines(Path file) throws IOException {
        return new ArrayList<>(Files.readAllLines(file));
    }

    /**
     * The run is the command `argv` builds, given to the program named (the arguments after `docker` itself): the fake writes them down, prints one
     * line and exits 3. The run says exit 3, not timed out, and the line; the user it passed is the working copy's owner
     * (nobody's when the scorer is root, since the tree is handed over first).
     */
    @Test
    void aRunIsTheArgvAndReportsItsExitAndOutput(@TempDir Path tools, @TempDir Path work, @TempDir Path out) throws IOException {
        Path rec = tools.resolve("args");
        String docker = fakeDocker(tools, "printf '%s\\n' \"$@\" > " + rec + "\necho 'tests ran'\nexit 3\n");
        Sandbox.Run run = new DockerSandbox(docker).run(Sandbox.Phase.INSTALL, work, out, "npm ci", Duration.ofSeconds(30));
        assertEquals(new Sandbox.Run(3, false, "tests ran\n"), run);
        List<String> got = lines(rec);
        String name = got.get(got.indexOf("--name") + 1);
        assertTrue(name.startsWith("evaluator-"), name);
        List<String> expected = DockerSandbox.argv(Sandbox.Phase.INSTALL, work, out, "npm ci", name, DockerSandbox.owner(work));
        expected.remove(0);
        assertEquals(expected, got);
    }

    /** Exit 0 is exit 0, not a timeout. */
    @Test
    void aRunThatSucceedsIsNotTimedOut(@TempDir Path tools, @TempDir Path work, @TempDir Path out) throws IOException {
        Sandbox.Run run = new DockerSandbox(fakeDocker(tools, "exit 0\n")).run(Sandbox.Phase.TEST, work, out, "x", Duration.ofSeconds(30));
        assertEquals(new Sandbox.Run(0, false, ""), run);
    }

    /** As root the working copy and the output folder go to nobody before the container starts, and the container is told that user. */
    @Test
    void asRootBothFoldersAreHandedOverBeforeTheRun(@TempDir Path tools, @TempDir Path work, @TempDir Path out) throws IOException {
        org.junit.jupiter.api.Assumptions.assumeTrue(((Integer) Files.getAttribute(work, "unix:uid")) == 0, "only root can hand a tree over");
        Files.writeString(work.resolve("a.py"), "x = 1\n");
        Files.writeString(out.resolve("r.xml"), "<r/>");
        Path rec = tools.resolve("args");
        new DockerSandbox(fakeDocker(tools, "printf '%s\\n' \"$@\" > " + rec + "\n")).run(Sandbox.Phase.TEST, work, out, "x", Duration.ofSeconds(30));
        assertEquals(65534, Files.getAttribute(work, "unix:uid"));
        assertEquals(65534, Files.getAttribute(work.resolve("a.py"), "unix:uid"));
        assertEquals(65534, Files.getAttribute(out, "unix:uid"));
        assertEquals(65534, Files.getAttribute(out.resolve("r.xml"), "unix:gid"));
        List<String> got = lines(rec);
        assertEquals("65534:65534", got.get(got.indexOf("--user") + 1));
    }

    /** A working copy that cannot be read (here, gone) is handed over as nobody's, fails, and the failure is thrown, not swallowed. */
    @Test
    void aWorkingCopyThatIsGoneIsAnError(@TempDir Path tools, @TempDir Path dir) throws IOException {
        DockerSandbox sandbox = new DockerSandbox(fakeDocker(tools, "exit 0\n"));
        assertThrows(java.io.UncheckedIOException.class, () -> sandbox.run(Sandbox.Phase.TEST, dir.resolve("gone"), dir, "x", Duration.ofSeconds(30)));
        assertEquals(DockerSandbox.NOBODY, DockerSandbox.owner(dir.resolve("gone")));
    }

    /** Past the timeout the run says -1 and timed out, with what it printed, and `docker kill <the container's name>` was asked. */
    @Test
    void aRunPastItsTimeoutIsKilledByName(@TempDir Path tools, @TempDir Path work, @TempDir Path out) throws IOException {
        Path rec = tools.resolve("args"), killed = tools.resolve("killed");
        String docker = fakeDocker(tools, "if [ \"$1\" = kill ]; then echo \"$2\" > " + killed + "; exit 0; fi\n"
                + "printf '%s\\n' \"$@\" > " + rec + "\necho started\nexec sleep 60\n");
        Sandbox.Run run = new DockerSandbox(docker).run(Sandbox.Phase.TEST, work, out, "x", Duration.ofSeconds(2));
        assertEquals(new Sandbox.Run(-1, true, "started\n"), run);
        List<String> got = lines(rec);
        assertEquals(List.of(got.get(got.indexOf("--name") + 1)), lines(killed));
    }

    /** An interrupted run says so, and keeps the interrupt for whoever called. */
    @Test
    void anInterruptedRunKeepsTheInterrupt(@TempDir Path tools, @TempDir Path work, @TempDir Path out) throws IOException {
        DockerSandbox sandbox = new DockerSandbox(fakeDocker(tools, "exit 0\n"));
        Thread.currentThread().interrupt();
        Sandbox.Run run;
        boolean kept;
        try {
            run = sandbox.run(Sandbox.Phase.TEST, work, out, "x", Duration.ofSeconds(30));
        } finally {
            kept = Thread.interrupted();
        }
        assertEquals(new Sandbox.Run(-1, true, "interrupted"), run);
        assertTrue(kept);
    }
}
