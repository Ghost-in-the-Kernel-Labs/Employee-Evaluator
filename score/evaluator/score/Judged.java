package evaluator.score;

import java.util.List;

/**
 * What a card says of the judgment it was measured through (TODO.md 2.2j, 2.2k): the hash of the prompt that asked, the models that answered,
 * and the notes the card's rules for the judged kind add (what was judged and how far to trust it, what is unverified and why). With no notes
 * a card reads as it did before 2.2k.
 */
public record Judged(String promptHash, List<String> models, List<String> notes) {
    public Judged {
        models = List.copyOf(models);
        notes = List.copyOf(notes);
    }

    public Judged(String promptHash, List<String> models) {
        this(promptHash, models, List.of());
    }
}
