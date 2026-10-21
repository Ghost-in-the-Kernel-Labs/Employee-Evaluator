package evaluator.analyze;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
}
