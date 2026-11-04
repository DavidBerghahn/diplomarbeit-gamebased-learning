package at.htlleonding.gamebasedlearning.games;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class Smart10SoloService {
    @Inject Smart10Feature feature;
    @Inject Smart10SnapshotLoader loader;
    @Inject CompletedRoundStore store;

    private final Map<UUID, SoloRound> rounds = new ConcurrentHashMap<>();

    public record CardView(UUID id, int position, String text) { }
    public record QuestionView(String prompt, String mode, List<CardView> cards) { }
    public record Start(UUID roundId, String gameId, String title, List<QuestionView> questions) { }
    public record Feedback(UUID cardId, boolean correct, Object solution, int correctCards,
                           int completedQuestions, int bestStreak, int score, boolean readyToFinish) { }
    public record Finish(UUID sessionId, int score) { }

    public Start start(UUID userId, String gameId) {
        feature.requireEnabled();
        if (userId == null) throw new IllegalArgumentException("Verified user required");
        Smart10Snapshot snapshot = loader.load(gameId);
        UUID roundId = UUID.randomUUID();
        rounds.put(roundId, new SoloRound(userId, snapshot));
        return new Start(roundId, gameId, snapshot.title(), snapshot.questions().stream()
                .map(question -> new QuestionView(question.prompt(), question.mode(), question.cards().stream()
                        .map(card -> new CardView(card.id(), card.position(), card.text())).toList())).toList());
    }

    public Feedback answer(UUID userId, UUID roundId, UUID cardId, JsonNode answer) {
        feature.requireEnabled();
        SoloRound round = own(userId, roundId);
        synchronized (round) {
            if (round.finishedAt != null || round.questionIndex >= round.snapshot.questions().size())
                throw new IllegalStateException("Round already finished");
            Smart10Snapshot.Group question = round.snapshot.question(round.questionIndex);
            Smart10Card card = round.snapshot.card(round.questionIndex, cardId);
            if (!round.answered.add(cardId)) throw new IllegalArgumentException("Card already answered");
            boolean correct = Smart10Cards.correct(question.mode(), card, answer);
            Instant answeredAt = Instant.now();
            round.answers.add(new CompletedRoundStore.Answer(question.questionVersionId(), cardId,
                    answer == null ? "null" : answer.toString(), correct, answeredAt));
            if (correct) {
                round.correctCards++;
                round.currentStreak++;
                round.bestStreak = Math.max(round.bestStreak, round.currentStreak);
            } else round.currentStreak = 0;
            if (round.answered.size() == 12) {
                round.completedQuestions++;
                round.questionIndex++;
                round.answered.clear();
            }
            return new Feedback(cardId, correct, card.solution(), round.correctCards,
                    round.completedQuestions, round.bestStreak, round.score(),
                    round.questionIndex == round.snapshot.questions().size());
        }
    }

    public Finish finish(UUID userId, UUID roundId) {
        feature.requireEnabled();
        SoloRound round = own(userId, roundId);
        synchronized (round) {
            if (round.questionIndex != round.snapshot.questions().size())
                throw new IllegalStateException("All cards must be answered before finish");
            if (round.finishedAt == null) {
                round.finishedAt = Instant.now();
                round.finalPayload = new CompletedRoundStore.Round(round.completionKey,
                        CompletedRoundStore.State.COMPLETED, round.snapshot.gameId(),
                        round.snapshot.gameVersionId(), null, "SINGLEPLAYER", round.startedAt,
                        round.finishedAt, List.of(), List.of(new CompletedRoundStore.Participant(
                                round.userId, null, round.score(), 1, round.startedAt,
                                List.copyOf(round.answers))));
            }
            UUID sessionId = store.saveSmart10(round.finalPayload);
            round.sessionId = sessionId;
            return new Finish(sessionId, round.score());
        }
    }

    public void cancel(UUID userId, UUID roundId) {
        feature.requireEnabled();
        SoloRound round = own(userId, roundId);
        synchronized (round) {
            if (round.finishedAt != null) throw new IllegalStateException("Finalized round cannot be cancelled");
            rounds.remove(roundId, round);
        }
    }

    private SoloRound own(UUID userId, UUID roundId) {
        SoloRound round = rounds.get(roundId);
        if (round == null || !round.userId.equals(userId))
            throw new IllegalArgumentException("Round not found for current user");
        return round;
    }

    private static final class SoloRound {
        final UUID userId;
        final Smart10Snapshot snapshot;
        final UUID completionKey = UUID.randomUUID();
        final Instant startedAt = Instant.now();
        final List<CompletedRoundStore.Answer> answers = new ArrayList<>();
        final Set<UUID> answered = new HashSet<>();
        int questionIndex;
        int correctCards;
        int completedQuestions;
        int currentStreak;
        int bestStreak;
        Instant finishedAt;
        UUID sessionId;
        CompletedRoundStore.Round finalPayload;

        SoloRound(UUID userId, Smart10Snapshot snapshot) { this.userId = userId; this.snapshot = snapshot; }
        int score() { return correctCards + completedQuestions * 10 + bestStreak * 2; }
    }
}
