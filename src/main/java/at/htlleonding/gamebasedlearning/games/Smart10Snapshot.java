package at.htlleonding.gamebasedlearning.games;

import java.util.List;
import java.util.UUID;

public record Smart10Snapshot(String gameId, UUID gameVersionId, String title,
                              List<Group> questions) {
    public record Group(UUID questionVersionId, String prompt, String mode,
                        List<Smart10Card> cards) { }

    public Group question(int index) { return questions.get(index); }
    public Smart10Card card(int questionIndex, UUID cardId) {
        return question(questionIndex).cards().stream().filter(card -> card.id().equals(cardId))
                .findFirst().orElseThrow(() -> new IllegalArgumentException("Card outside pinned question"));
    }
}
