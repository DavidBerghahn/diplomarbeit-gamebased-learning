package at.htlleonding.gamebasedlearning.games;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class Smart10ContentTest {
    @Inject GameRepository games;
    @Inject Smart10SnapshotLoader loader;

    @Test
    void storesAllModesAndKeepsPinnedVersionsAfterEditCopyAndDelete() {
        Game created = games.create(game("smart-content-" + UUID.randomUUID()), null);
        Smart10Snapshot first = loader.load(created.id);
        assertEquals(3, first.questions().size());
        for (Smart10Snapshot.Group group : first.questions()) assertEquals(12, group.cards().size());
        assertEquals(Boolean.TRUE, first.questions().get(0).cards().get(0).solution());
        assertEquals("Antwort 1", first.questions().get(1).cards().get(0).solution());
        assertEquals(12, first.questions().get(2).cards().get(0).solution());

        Game edited = games.findById(created.id).orElseThrow();
        Question changed = edited.questions.get(0);
        Smart10Card old = changed.smart10Cards.get(0);
        changed.smart10Cards.set(0, new Smart10Card(old.id(), 1, "Geändert", false));
        Game next = games.update(created.id, edited).orElseThrow();
        assertNotEquals(first.gameVersionId(), next.currentVersionId);
        Smart10Snapshot second = loader.load(created.id);
        assertEquals("Geändert", second.questions().get(0).cards().get(0).text());
        assertNotEquals(first.questions().get(0).cards().get(0).id(), second.questions().get(0).cards().get(0).id());
        assertEquals("Karte 1", first.questions().get(0).cards().get(0).text());

        Game copy = games.copyGame(created.id, "smart-copy-" + UUID.randomUUID(), null);
        assertNotEquals(second.questions().get(0).cards().get(0).id(),
                loader.load(copy.id).questions().get(0).cards().get(0).id());
        assertTrue(games.delete(created.id));
        assertEquals(copy.id, loader.load(copy.id).gameId());
    }

    @Test
    void rejectsIncompleteAndInvalidOrderingCards() {
        Game missing = game("missing-" + UUID.randomUUID());
        missing.questions.get(0).smart10Cards.removeLast();
        assertThrows(IllegalArgumentException.class, () -> games.create(missing, null));

        Game duplicate = game("duplicate-" + UUID.randomUUID());
        Question ordering = duplicate.questions.get(2);
        ordering.smart10Cards.set(0, new Smart10Card(null, 1, "Karte 1", 1));
        assertThrows(IllegalArgumentException.class, () -> games.create(duplicate, null));

        for (int invalid : List.of(0, 13)) {
            Game outOfRange = game("range-" + invalid + UUID.randomUUID());
            outOfRange.questions.get(2).smart10Cards.set(0, new Smart10Card(null, 1, "Karte 1", invalid));
            assertThrows(IllegalArgumentException.class, () -> games.create(outOfRange, null));
        }
        Game wrongType = game("wrong-type-" + UUID.randomUUID());
        wrongType.questions.get(0).smart10Cards.set(0, new Smart10Card(null, 1, "Karte 1", "true"));
        assertThrows(IllegalArgumentException.class, () -> games.create(wrongType, null));
    }

    private Game game(String id) {
        Game game = new Game();
        game.id = id;
        game.gameType = "Smart10";
        game.teacher = "Test";
        game.name = "Smart10 Test";
        game.branches = List.of("SSE");
        game.questions = new ArrayList<>();
        for (String mode : List.of("TRUE_FALSE", "FREE_TEXT", "ORDERING")) {
            Question question = new Question();
            question.type = mode;
            question.text = "Frage " + mode;
            question.smart10Cards = new ArrayList<>();
            for (int i = 1; i <= 12; i++) {
                Object solution = switch (mode) {
                    case "TRUE_FALSE" -> i % 2 == 1;
                    case "FREE_TEXT" -> "Antwort " + i;
                    default -> 13 - i;
                };
                question.smart10Cards.add(new Smart10Card(null, i, "Karte " + i, solution));
            }
            game.questions.add(question);
        }
        return game;
    }
}
