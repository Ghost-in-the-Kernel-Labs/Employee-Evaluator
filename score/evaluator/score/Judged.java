package evaluator.score;

import java.util.List;

/** What a card says of the judgment it was measured through (TODO.md 2.2j): the hash of the prompt that asked, and the models that answered. */
public record Judged(String promptHash, List<String> models) {
    public Judged {
        models = List.copyOf(models);
    }
}
