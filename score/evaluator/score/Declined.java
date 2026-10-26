package evaluator.score;

import evaluator.rubric.Json;
import java.util.stream.Collectors;

/**
 * The outcome of a submission this platform does not assess (E-056, E-086): no card, and a respectful message that says why. It is printed in place of
 * a card, as JSON or as Markdown, with the judgment it rests on. It is not a refusal of the command: nothing about its use was wrong.
 */
// Decision: E-056
public final class Declined {
    /** The exit status of a command that declined a submission: not 0, as no card was made, and not 1, as nothing about the use was wrong (E-086). */
    public static final int EXIT_STATUS = 2;

    /** E-056's message: what the platform assesses, and that a traditional code review would serve the applicant better. It judges nobody's ability. */
    public static final String MESSAGE = "This platform assesses AI-augmented development: how well a developer structures work built with AI assistance. "
            + "This repository reads as generated wholesale by a tool, with little of the applicant's own work, so it is outside what the platform measures "
            + "and no score card was made. A traditional code review would serve you better here. This says nothing about your ability.";

    private Declined() {}

    /** The declined outcome as JSON, one object ending in a newline: the outcome, the kind the judge named, the message and the judgment it rests on. */
    public static String json(String kind, Judged judged) {
        return "{\n  \"outcome\": \"declined\",\n  \"kind\": " + Json.quote(kind) + ",\n  \"message\": " + Json.quote(MESSAGE)
                + ",\n  \"judged\": " + CardJson.judged(judged) + "\n}\n";
    }

    /** The declined outcome as Markdown: a heading, the message and the judgment line. */
    public static String markdown(String kind, Judged judged) {
        return "# No score card\n\n" + CardMarkdown.escape(MESSAGE) + "\n\n- Kind: " + CardMarkdown.code(kind) + "\n- Judged: prompt " + CardMarkdown.code(judged.promptHash())
                + ", models " + judged.models().stream().map(CardMarkdown::code).collect(Collectors.joining(", ")) + "\n";
    }
}
