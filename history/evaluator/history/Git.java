package evaluator.history;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * git, run over a submission's repository as over hostile input (E-013, E-033): its hooks off, and every
 * key its own `.git/config` could set to run a command overridden (the fsmonitor, a pager, an external
 * diff); no system or user config read; nothing fetched; a time limit and a cap on what it may print.
 * git reads the repository; nothing in it runs.
 */
// Decision: E-033
final class Git {
    /** The most of git's output read; past it the history is refused, not read in part. */
    static final int MAX_OUTPUT = 64 * 1024 * 1024;
    static final long TIMEOUT_SECONDS = 120;
    /** The overrides every invocation carries, ahead of its own arguments. */
    static final List<String> SAFE = List.of(
            "-c", "core.hooksPath=/dev/null", "-c", "core.fsmonitor=false", "-c", "core.pager=cat",
            "-c", "core.untrackedCache=false", "-c", "diff.external=", "-c", "protocol.allow=never",
            "-c", "safe.directory=*", "--no-pager");

    private Git() {}

    /** git's output for these arguments in the repository at repo; an IOException when it fails, times out or prints too much. */
    static String run(Path repo, List<String> args) throws IOException {
        Process git = builder(repo, args).start();
        CompletableFuture<byte[]> out = CompletableFuture.supplyAsync(() -> readCapped(git.getInputStream()), Thread::startVirtualThread);
        try {
            byte[] bytes = out.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!git.waitFor(5, TimeUnit.SECONDS)) throw new IOException("git did not exit");
            if (bytes == null) throw new IOException("git printed over " + MAX_OUTPUT + " bytes");
            if (git.exitValue() != 0) throw new IOException("git " + args.get(0) + " failed (exit " + git.exitValue() + ")");
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (TimeoutException e) {
            throw new IOException("git ran past " + TIMEOUT_SECONDS + " s");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("interrupted");
        } catch (ExecutionException e) {
            throw new IOException("git's output could not be read", e);
        } finally {
            git.destroyForcibly();
        }
    }

    /** The command and an environment of git's own: no config but the repository's, its dangerous keys overridden. */
    static ProcessBuilder builder(Path repo, List<String> args) {
        List<String> command = new ArrayList<>(List.of("git", "-C", repo.toAbsolutePath().toString()));
        command.addAll(SAFE);
        command.addAll(args);
        ProcessBuilder builder = new ProcessBuilder(command).redirectError(ProcessBuilder.Redirect.DISCARD);
        scrub(builder.environment());
        return builder;
    }

    /** The environment git runs in: every GIT_ variable of ours dropped, then git's own settings put (no config, no prompt, no locks). */
    static void scrub(Map<String, String> env) {
        env.keySet().removeIf(key -> key.startsWith("GIT_"));
        env.put("GIT_CONFIG_NOSYSTEM", "1");
        env.put("GIT_CONFIG_GLOBAL", "/dev/null");
        env.put("GIT_TERMINAL_PROMPT", "0");
        env.put("GIT_OPTIONAL_LOCKS", "0");
        env.put("HOME", "/nonexistent");
        env.put("LC_ALL", "C");
    }

    /** Everything the stream holds, or null once it passes MAX_OUTPUT. */
    static byte[] readCapped(InputStream in) {
        try (in) {
            byte[] bytes = in.readNBytes(MAX_OUTPUT + 1);
            return bytes.length > MAX_OUTPUT ? null : bytes;
        } catch (IOException e) {
            throw new java.io.UncheckedIOException(e);
        }
    }
}
