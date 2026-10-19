package evaluator.analyze;

import java.nio.file.Path;
import java.time.Duration;

/**
 * Where a submission's code runs (E-037): one script, in one phase, over a working copy mounted at
 * `/work` and an output folder at `/out`, with a time limit. The kernel never runs a submission's
 * code any other way; a test stands a fake in for Docker.
 */
public interface Sandbox {
    /** INSTALL reaches the package registries and nothing else; TEST reaches nothing. */
    enum Phase { INSTALL, TEST }

    /** How a run ended: its exit status, whether the time limit stopped it, and the end of what it printed. */
    record Run(int exit, boolean timedOut, String output) {}

    /** Runs a `sh -c` script in the phase's container over work and out, stopping it at the timeout. */
    Run run(Phase phase, Path work, Path out, String script, Duration timeout);
}
