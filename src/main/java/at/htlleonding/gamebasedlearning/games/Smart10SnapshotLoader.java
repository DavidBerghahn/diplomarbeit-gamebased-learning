package at.htlleonding.gamebasedlearning.games;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.List;

@ApplicationScoped
public class Smart10SnapshotLoader {
    @Inject GameRepository games;

    @Transactional
    public Smart10Snapshot load(String gameId) {
        Game game = games.findById(gameId)
                .orElseThrow(() -> new IllegalArgumentException("Stored game not found"));
        if (!"Smart10".equals(game.gameType) || game.questions.isEmpty())
            throw new IllegalArgumentException("Stored Smart10 game required");
        List<Smart10Snapshot.Group> questions = game.questions.stream().map(question -> {
            Smart10Cards.validate(question);
            if (question.currentVersionId == null)
                throw new IllegalArgumentException("Unversioned Smart10 question");
            return new Smart10Snapshot.Group(question.currentVersionId, question.text, question.type,
                    List.copyOf(question.smart10Cards));
        }).toList();
        return new Smart10Snapshot(game.id, game.currentVersionId, game.name, questions);
    }
}
