package at.htlleonding.gamebasedlearning.games;

import at.htlleonding.gamebasedlearning.users.AppUser;
import at.htlleonding.gamebasedlearning.users.UserRole;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@TestProfile(SampleGamesTest.SamplesOnly.class)
class SampleGamesTest {
    public static class SamplesOnly implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                    "gamebased.demo-data.enabled", "false",
                    "gamebased.sample-games.enabled", "true",
                    "quarkus.datasource.db-kind", "h2",
                    "quarkus.datasource.jdbc.url", "jdbc:h2:mem:sample_games_only;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
                    "quarkus.flyway.locations", "db/migration/h2");
        }
    }

    @Inject GameRepository games;
    @Inject GameDemoData seeder;
    @Inject Smart10SnapshotLoader loader;
    @Inject Smart10SoloService solo;
    @Inject CompletedRoundStore results;
    @Inject ObjectMapper mapper;

    @Test
    void createsExactlyTwoPublicPlayableSamplesWithoutLegacyDemos() {
        var catalog = games.findAll();
        assertEquals(Set.of("sample-smart10-programming", "sample-just-one-programming"),
                catalog.stream().map(game -> game.id).collect(java.util.stream.Collectors.toSet()));
        assertTrue(catalog.stream().allMatch(game -> GameAccess.publicGame(game) && game.playable()));
        assertEquals(12, loader.load("sample-smart10-programming").questions().getFirst().cards().size());
        var justOne = games.findById("sample-just-one-programming").orElseThrow();
        assertNotNull(justOne.currentVersionId);
        assertEquals("JUST_ONE", justOne.questions.getFirst().type);
        assertEquals(5, justOne.questions.getFirst().answerOptions.size());
    }

    @Test
    @TestTransaction
    void repeatedStartupPreservesEditedGamesAndDoesNotAddVersionsOrDuplicates() {
        var smart10 = games.findById("sample-smart10-programming").orElseThrow();
        smart10.name = "Vom Team angepasst";
        var edited = games.update(smart10.id, smart10).orElseThrow();
        var justOneVersion = games.findById("sample-just-one-programming").orElseThrow().currentVersionId;
        seeder.seed(new StartupEvent());
        seeder.seed(new StartupEvent());
        var unchanged = games.findById(edited.id).orElseThrow();
        assertEquals("Vom Team angepasst", unchanged.name);
        assertEquals(edited.currentVersionId, unchanged.currentVersionId);
        assertEquals(justOneVersion, games.findById("sample-just-one-programming").orElseThrow().currentVersionId);
        assertEquals(2, games.findAll().size());
    }

    @Test
    void storedSmart10SampleSavesACompletedRoundExactlyOnce() {
        var student = new AppUser();
        student.externalSubject = "sample-test:" + UUID.randomUUID();
        student.username = "sample-player";
        student.displayName = "Beispielspieler";
        student.schoolClass = "5TEST";
        student.role = UserRole.STUDENT;
        io.quarkus.narayana.jta.QuarkusTransaction.requiringNew().run(student::persist);
        var started = solo.start(student.id, "sample-smart10-programming");
        var cards = started.questions().getFirst().cards();
        for (int index = 0; index < cards.size(); index++) {
            assertTrue(solo.answer(student.id, started.roundId(), cards.get(index).id(),
                    mapper.getNodeFactory().booleanNode(index < 6)).correct());
        }
        var finished = solo.finish(student.id, started.roundId());
        assertNotNull(finished.sessionId());
        assertEquals(46, finished.score());
        assertEquals(finished.sessionId(), solo.finish(student.id, started.roundId()).sessionId());
        assertEquals(46, results.totalPoints(student.id));
    }
}
