package evaluator.score;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

/** The declined outcome (TODO.md 2.2k, E-056, E-086): a respectful message in place of a card, in JSON and in Markdown, and its exit status. */
class DeclinedTest {
    private static final Judged JUDGED = new Judged("ab12", List.of("m-1", "m\"2"), List.of("kind: generated, declined (agreement 27 of 30)"));

    @Test
    void theMessageSaysWhatThePlatformAssessesAndPointsToAReview() {
        assertTrue(Declined.MESSAGE.contains("assesses AI-augmented development"), Declined.MESSAGE);
        assertTrue(Declined.MESSAGE.contains("A traditional code review would serve you better here."), Declined.MESSAGE);
        assertTrue(Declined.MESSAGE.contains("says nothing about your ability"), Declined.MESSAGE);
        assertTrue(Declined.MESSAGE.contains("no score card was made"), Declined.MESSAGE);
    }

    @Test
    void theJsonIsOneObjectWithTheOutcomeTheKindTheMessageAndTheJudgment() {
        assertEquals("{\n  \"outcome\": \"declined\",\n  \"kind\": \"generated\",\n  \"message\": \"" + Declined.MESSAGE
                + "\",\n  \"judged\": {\"promptHash\": \"ab12\", \"models\": [\"m-1\", \"m\\\"2\"], \"notes\": [\"kind: generated, declined (agreement 27 of 30)\"]}\n}\n",
                Declined.json("generated", JUDGED));
    }

    @Test
    void theMarkdownIsAHeadingTheMessageAndTheJudgmentLine() {
        assertEquals("# No score card\n\n" + CardMarkdown.escape(Declined.MESSAGE) + "\n\n- Kind: `generated`\n- Judged: prompt `ab12`, models `m-1`, `m\"2`\n",
                Declined.markdown("generated", JUDGED));
    }

    /** Not 0, as no card was made, and not 1, as nothing about the use was wrong. */
    @Test
    void theExitStatusIsItsOwn() {
        assertEquals(2, Declined.EXIT_STATUS);
    }
}
