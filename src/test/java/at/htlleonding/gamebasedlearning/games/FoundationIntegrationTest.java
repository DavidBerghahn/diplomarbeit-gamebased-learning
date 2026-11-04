package at.htlleonding.gamebasedlearning.games;

import at.htlleonding.gamebasedlearning.users.AppUser;
import at.htlleonding.gamebasedlearning.users.UserRole;
import at.htlleonding.gamebasedlearning.auth.AuthProvider;
import at.htlleonding.gamebasedlearning.auth.AuthenticatedUser;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusMock;
import io.quarkus.test.junit.QuarkusTest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class FoundationIntegrationTest {
    @Inject GameRepository games;
    @Inject CompletedRoundStore rounds;
    @Inject EntityManager em;
    @Inject GameSocket socket;
    @Inject ObjectMapper mapper;

    @Test
    void restRoundTripsKeepQuestionIdentityReportsTagsAndHiddenSolutions() throws Exception {
        Game draft = input("rest-roundtrip-" + UUID.randomUUID());
        Question second = new Question();
        second.type = "TRUE_FALSE";
        second.text = "Zweite Frage?";
        second.trueFalseCorrect = true;
        draft.questions.add(second);
        Game created = games.create(draft, null);
        Long firstId = created.questions.get(0).id;
        Long secondId = created.questions.get(1).id;
        UUID firstQuestionVersion = versionOfFirstQuestion(created.currentVersionId);
        AppUser reporter = user("restreporter");
        UUID report = games.reportQuestion(created.id, firstId, reporter, "Bitte prüfen");
        QuarkusMock.installMockForType(new AuthProvider() {
            @Override public AuthenticatedUser currentUser(jakarta.ws.rs.core.HttpHeaders headers) {
                return new AuthenticatedUser("rest-editor", "rest-editor", "Editor", null, UserRole.TEACHER);
            }
        }, AuthProvider.class);

        ObjectNode body = (ObjectNode) mapper.readTree(io.restassured.RestAssured.given()
                .get("/api/games/{id}", created.id).then().statusCode(200).extract().asString());
        assertEquals(firstId.longValue(), body.path("fragen").get(0).path("id").asLong());
        body.put("name", "Neuer Titel");
        io.restassured.RestAssured.given().contentType("application/json").body(body.toString())
                .put("/api/games/{id}", created.id).then().statusCode(200);
        em.clear();
        Game titled = games.findById(created.id).orElseThrow();
        assertEquals(List.of(firstId, secondId), titled.questions.stream().map(q -> q.id).toList());
        assertEquals(firstQuestionVersion, versionOfFirstQuestion(titled.currentVersionId));
        assertTrue(titled.tags.containsAll(List.of("Programmieren", "Grundlagen")));
        assertEquals(Boolean.TRUE, titled.questions.get(1).trueFalseCorrect);

        body = (ObjectNode) mapper.readTree(io.restassured.RestAssured.given()
                .get("/api/games/{id}", created.id).then().statusCode(200).extract().asString());
        ArrayNode questions = (ArrayNode) body.path("fragen");
        ObjectNode first = (ObjectNode) questions.remove(0);
        ObjectNode moved = (ObjectNode) questions.get(0);
        moved.remove("id");
        first.remove("id");
        questions.add(first);
        assertEquals("Zweite Frage?", questions.get(0).path("frage").asText());
        io.restassured.RestAssured.given().contentType("application/json").body(body.toString())
                .put("/api/games/{id}", created.id).then().statusCode(200);
        em.clear();
        Game reordered = games.findById(created.id).orElseThrow();
        assertEquals(List.of(secondId, firstId), reordered.questions.stream().map(q -> q.id).toList());
        assertEquals(1L, count("select count(*) from question_reports where id = ?1", report));
        assertEquals(Boolean.TRUE, reordered.questions.get(0).trueFalseCorrect);

        body = (ObjectNode) mapper.readTree(io.restassured.RestAssured.given()
                .get("/api/games/{id}", created.id).then().statusCode(200).extract().asString());
        questions = (ArrayNode) body.path("fragen");
        for (JsonNode question : questions) ((ObjectNode) question).remove("id");
        ((ObjectNode) questions.get(0)).put("frage", "Bearbeitete zweite Frage?");
        io.restassured.RestAssured.given().contentType("application/json").body(body.toString())
                .put("/api/games/{id}", created.id).then().statusCode(200);
        em.clear();
        Game edited = games.findById(created.id).orElseThrow();
        assertEquals(List.of(secondId, firstId), edited.questions.stream().map(q -> q.id).toList());
        assertEquals(Boolean.TRUE, edited.questions.get(0).trueFalseCorrect);
        assertEquals(1L, count("select count(*) from question_reports where id = ?1", report));

        edited.tags = List.of("Gezielt geändert");
        Game retagged = games.update(created.id, edited).orElseThrow();
        assertTrue(retagged.tags.contains("Gezielt geändert"));
        assertFalse(retagged.tags.contains("Grundlagen"));
    }

    @Test
    void rejectsAmbiguousIdlessEditsAndForeignQuestionIds() throws Exception {
        Game draft = input("rest-ambiguous-" + UUID.randomUUID());
        Question second = new Question();
        second.type = "single_choice";
        second.text = "Zweite Frage?";
        draft.questions.add(second);
        Game game = games.create(draft, null);
        Game foreign = games.create(input("rest-foreign-" + UUID.randomUUID()), null);
        UUID before = game.currentVersionId;
        Game idless = mapper.readValue(mapper.writeValueAsString(game), Game.class);
        for (Question question : idless.questions) question.id = null;
        idless.questions.get(0).text = "Bearbeitet eins";
        idless.questions.get(1).text = "Bearbeitet zwei";
        assertThrows(IllegalArgumentException.class, () -> games.update(game.id, idless));
        assertEquals(before, games.findById(game.id).orElseThrow().currentVersionId);
        idless.questions.get(0).id = foreign.questions.getFirst().id;
        assertThrows(IllegalArgumentException.class, () -> games.update(game.id, idless));
    }

    @Test
    void snapshotsCopyTagsAndDeletionKeepHistory() {
        Game created = games.create(input("foundation-" + UUID.randomUUID()), null);
        UUID firstVersion = created.currentVersionId;
        Long originalQuestion = created.questions.getFirst().id;
        assertEquals(List.of("DDP", "SSE"), created.branches);
        assertTrue(created.tags.containsAll(List.of("Programmieren", "Grundlagen")));

        Game edited = games.findById(created.id).orElseThrow();
        edited.branches = List.of("SSE", "CSI");
        edited.questions.getFirst().text = "Neue Frage?";
        edited.questions.getFirst().answerOptions.getFirst().text = "Neue Antwort";
        Game second = games.update(created.id, edited).orElseThrow();
        assertNotEquals(firstVersion, second.currentVersionId);
        assertEquals(originalQuestion, second.questions.getFirst().id);
        assertEquals(List.of("SSE", "CSI"), second.branches);
        assertEquals("Neue Antwort", second.questions.getFirst().answerOptions.getFirst().text);
        assertEquals(List.of("SSE", "CSI"), strings(
                "select branch from game_branches where game_id = ?1 order by branch_order", created.id));
        assertEquals(1L, count("select count(*) from answer_options where question_id = ?1", originalQuestion));
        assertEquals(List.of("DDP", "SSE"), strings(
                "select branch from game_version_branches where game_version_id = ?1 order by position", firstVersion));
        assertEquals("Alte Frage?", scalarString(
                "select qv.text from game_version_questions gvq join question_versions qv "
                + "on qv.id = gvq.question_version_id where gvq.game_version_id = ?1", firstVersion));
        assertEquals("Richtig", scalarString(
                "select ao.text from game_version_questions gvq join answer_options ao "
                + "on ao.question_version_id = gvq.question_version_id "
                + "where gvq.game_version_id = ?1 and ao.answer_order = 0", firstVersion));

        AppUser reporter = user("copyreporter");
        UUID originalReport = games.reportQuestion(created.id, originalQuestion, reporter, "Bitte prüfen");
        Game copy = games.copyGame(created.id, "copy-" + UUID.randomUUID(), null);
        assertEquals(created.familyId, copy.familyId);
        assertEquals(created.id, copy.copiedFromGameId);
        assertEquals(created.gameType, copy.gameType);
        assertNotEquals(originalQuestion, copy.questions.getFirst().id);
        assertEquals(0L, count("select count(*) from question_reports where question_version_id in "
                + "(select id from question_versions where question_id = ?1)", copy.questions.getFirst().id));
        assertEquals(1L, count("select count(*) from question_reports where id = ?1", originalReport));
        assertTrue(games.delete(created.id));
        assertEquals(1L, count("select count(*) from games where id = ?1 and deleted_at is not null", created.id));
        assertTrue(games.findById(created.id).isEmpty());
        assertTrue(games.findById(copy.id).isPresent());
        assertEquals(1L, count("select count(*) from game_versions where id = ?1", firstVersion));

        Game invalid = games.findById(copy.id).orElseThrow();
        invalid.questions.getFirst().id = Long.MAX_VALUE;
        UUID before = copy.currentVersionId;
        assertThrows(IllegalArgumentException.class, () -> games.update(copy.id, invalid));
        assertEquals(before, games.findById(copy.id).orElseThrow().currentVersionId);
    }

    @Test
    void restAndSocketKeepLegacyJsonFields() throws Exception {
        Game game = games.create(input("json-" + UUID.randomUUID()), null);
        String socketJson = socket.onMessage(mapper.writeValueAsString(
                java.util.Map.of("type", "get_game", "id", game.id, "requestId", "r1")), null);
        JsonNode fromSocket = mapper.readTree(socketJson).path("data");
        assertEquals(game.id, fromSocket.path("id").asText());
        assertEquals("Quizbattle", fromSocket.path("spiel_typ").asText());
        assertEquals("Programmieren", fromSocket.path("fach").asText());
        assertEquals("DDP", fromSocket.path("zweige").get(0).asText());
        assertEquals("Alte Frage?", fromSocket.path("fragen").get(0).path("frage").asText());
        assertEquals("Richtig", fromSocket.path("fragen").get(0)
                .path("antwortmoeglichkeiten").get(0).path("text").asText());
        Game decoded = mapper.treeToValue(fromSocket, Game.class);
        assertEquals(List.of("DDP", "SSE"), decoded.branches);
        assertEquals("Alte Frage?", decoded.questions.getFirst().text);
        assertEquals("Richtig", decoded.questions.getFirst().answerOptions.getFirst().text);
        io.restassured.RestAssured.given().get("/api/games/{id}", game.id)
                .then().statusCode(200).body("id", org.hamcrest.Matchers.equalTo(game.id))
                .body("zweige[0]", org.hamcrest.Matchers.equalTo("DDP"))
                .body("fragen[0].frage", org.hamcrest.Matchers.equalTo("Alte Frage?"));
    }

    @Test
    void questionSolutionsAndReportDeletionPreservePlayedHistory() {
        Game input = input("solutions-" + UUID.randomUUID());
        Question yesNo = new Question();
        yesNo.type = "TRUE_FALSE";
        yesNo.text = "Ist das wahr?";
        yesNo.trueFalseCorrect = true;
        Question ordering = new Question();
        ordering.type = "ORDERING";
        ordering.text = "Sortieren";
        ordering.orderingItems = List.of("eins", "zwei");
        Question freeText = new Question();
        freeText.type = "FREE_TEXT";
        freeText.text = "Antwort?";
        freeText.acceptedTextAnswers = List.of("Ja", "ja");
        input.questions.addAll(List.of(yesNo, ordering, freeText));
        Game game = games.create(input, null);
        assertEquals(Boolean.TRUE, game.questions.get(1).trueFalseCorrect);
        assertEquals(List.of("eins", "zwei"), game.questions.get(2).orderingItems);
        assertEquals(List.of("Ja", "ja"), game.questions.get(3).acceptedTextAnswers);

        AppUser player = user("reporter");
        UUID firstVersion = versionOfFirstQuestion(game.currentVersionId);
        Instant now = Instant.parse("2026-11-03T12:00:00Z");
        CompletedRoundStore.Round round = new CompletedRoundStore.Round(UUID.randomUUID(),
                CompletedRoundStore.State.COMPLETED, game.id, game.currentVersionId, null,
                "SINGLEPLAYER", now, now.plusSeconds(20), List.of(),
                List.of(new CompletedRoundStore.Participant(player.id, null, 10, 1, now,
                        List.of(new CompletedRoundStore.Answer(firstVersion, "Richtig", true, now.plusSeconds(10))))));
        UUID session = rounds.save(round);
        UUID report = games.reportQuestion(game.id, game.questions.getFirst().id, player, "Bitte prüfen");
        assertTrue(games.deleteQuestion(game.id, game.questions.getFirst().id));
        assertEquals(0L, count("select count(*) from question_reports where id = ?1", report));
        assertEquals(1L, count("select count(*) from question_versions where id = ?1", firstVersion));
        assertEquals(1L, count("select count(*) from player_answers where question_version_id = ?1", firstVersion));
        assertEquals(1L, count("select count(*) from game_sessions where id = ?1", session));
        assertEquals(3, games.findById(game.id).orElseThrow().questions.size());
    }

    @Test
    void completedRoundsAreAtomicAndIdempotent() {
        Game game = games.create(input("round-" + UUID.randomUUID()), null);
        UUID questionVersion = versionOfFirstQuestion(game.currentVersionId);
        AppUser host = user("host");
        AppUser first = user("first");
        AppUser second = user("second");
        Instant now = Instant.parse("2026-11-03T12:00:00Z");
        CompletedRoundStore.Round round = new CompletedRoundStore.Round(
                UUID.randomUUID(), CompletedRoundStore.State.COMPLETED,
                game.id, game.currentVersionId, host.id, "MULTIPLAYER", now, now.plusSeconds(100),
                List.of(new CompletedRoundStore.Team("A", 12, 1),
                        new CompletedRoundStore.Team("B", 8, 2)),
                List.of(new CompletedRoundStore.Participant(first.id, "A", 100, 1, now,
                                List.of(new CompletedRoundStore.Answer(questionVersion, "ja", true, now.plusSeconds(50)))),
                        new CompletedRoundStore.Participant(second.id, "B", 50, 2, now,
                                List.of(new CompletedRoundStore.Answer(questionVersion, "nein", false, now.plusSeconds(51))))));
        UUID saved = rounds.save(round);
        assertEquals(saved, rounds.save(round));
        assertEquals(1L, count("select count(*) from game_sessions where completion_key = ?1", round.completionKey()));
        assertEquals(2L, count("select count(*) from session_participants where session_id = ?1", saved));
        assertEquals(2L, count("select count(*) from player_answers where participant_id in "
                + "(select id from session_participants where session_id = ?1)", saved));
        assertEquals(100L, rounds.totalPoints(first.id));
        assertEquals(0L, rounds.totalPoints(host.id));
        assertEquals(List.of(first.id), rounds.winnerUsers(saved));

        CompletedRoundStore.Round changed = new CompletedRoundStore.Round(
                round.completionKey(), round.state(), round.gameId(), round.gameVersionId(),
                round.hostUserId(), round.mode(), round.startedAt(), round.finishedAt(),
                round.teams(), List.of(new CompletedRoundStore.Participant(first.id, "A", 999, 1, now,
                        List.of(new CompletedRoundStore.Answer(questionVersion, "ja", true, now.plusSeconds(50))))));
        assertThrows(IllegalArgumentException.class, () -> rounds.save(changed));
        assertEquals(100L, rounds.totalPoints(first.id));

        CompletedRoundStore.Round aborted = new CompletedRoundStore.Round(
                UUID.randomUUID(), CompletedRoundStore.State.ABORTED, game.id, game.currentVersionId,
                null, "SINGLEPLAYER", now, now.plusSeconds(100), List.of(),
                List.of(new CompletedRoundStore.Participant(first.id, null, 10, 1, now, List.of())));
        assertThrows(IllegalArgumentException.class, () -> rounds.save(aborted));
        assertEquals(0L, count("select count(*) from game_sessions where completion_key = ?1", aborted.completionKey()));
    }

    @Test
    void rejectsForeignSnapshotHostAndDuplicateParticipants() {
        Game game = games.create(input("negative-" + UUID.randomUUID()), null);
        Game other = games.create(input("other-" + UUID.randomUUID()), null);
        AppUser player = user("player");
        UUID otherQuestion = versionOfFirstQuestion(other.currentVersionId);
        Instant now = Instant.parse("2026-11-03T12:00:00Z");
        CompletedRoundStore.Participant participant = new CompletedRoundStore.Participant(
                player.id, null, 10, 1, now,
                List.of(new CompletedRoundStore.Answer(otherQuestion, "x", false, now)));
        CompletedRoundStore.Round foreign = new CompletedRoundStore.Round(
                UUID.randomUUID(), CompletedRoundStore.State.COMPLETED,
                game.id, game.currentVersionId, null, "SINGLEPLAYER", now, now.plusSeconds(1),
                List.of(), List.of(participant));
        assertThrows(IllegalArgumentException.class, () -> rounds.save(foreign));
        assertEquals(0L, count("select count(*) from game_sessions where completion_key = ?1", foreign.completionKey()));
        CompletedRoundStore.Round host = new CompletedRoundStore.Round(
                UUID.randomUUID(), CompletedRoundStore.State.COMPLETED,
                game.id, game.currentVersionId, player.id, "SINGLEPLAYER", now, now.plusSeconds(1),
                List.of(), List.of(participant));
        assertThrows(IllegalArgumentException.class, () -> rounds.save(host));
        CompletedRoundStore.Round duplicate = new CompletedRoundStore.Round(
                UUID.randomUUID(), CompletedRoundStore.State.COMPLETED,
                game.id, game.currentVersionId, null, "SINGLEPLAYER", now, now.plusSeconds(1),
                List.of(), List.of(participant, participant));
        assertThrows(IllegalArgumentException.class, () -> rounds.save(duplicate));
        CompletedRoundStore.Round foreignTeam = new CompletedRoundStore.Round(
                UUID.randomUUID(), CompletedRoundStore.State.COMPLETED,
                game.id, game.currentVersionId, null, "MULTIPLAYER", now, now.plusSeconds(1),
                List.of(new CompletedRoundStore.Team("A", 5, 1)),
                List.of(new CompletedRoundStore.Participant(player.id, "B", 10, 1, now, List.of())));
        assertThrows(IllegalArgumentException.class, () -> rounds.save(foreignTeam));
        CompletedRoundStore.Round unverified = new CompletedRoundStore.Round(
                UUID.randomUUID(), CompletedRoundStore.State.COMPLETED,
                game.id, game.currentVersionId, null, "SINGLEPLAYER", now, now.plusSeconds(1),
                List.of(), List.of(new CompletedRoundStore.Participant(UUID.randomUUID(), null, 10, 1, now, List.of())));
        assertThrows(IllegalArgumentException.class, () -> rounds.save(unverified));
    }

    @Test
    void rejectsUnknownAndInconsistentSingleplayerModesButAllowsMultiplayerWithoutTeams() {
        Game game = games.create(input("modes-" + UUID.randomUUID()), null);
        AppUser first = user("modefirst");
        AppUser second = user("modesecond");
        Instant now = Instant.parse("2026-11-04T12:00:00Z");
        CompletedRoundStore.Participant firstResult = new CompletedRoundStore.Participant(
                first.id, null, 10, 1, now, List.of());
        CompletedRoundStore.Participant secondResult = new CompletedRoundStore.Participant(
                second.id, null, 5, 2, now, List.of());
        CompletedRoundStore.Round unknown = new CompletedRoundStore.Round(UUID.randomUUID(),
                CompletedRoundStore.State.COMPLETED, game.id, game.currentVersionId,
                null, "SOLO", now, now.plusSeconds(10), List.of(), List.of(firstResult));
        assertThrows(IllegalArgumentException.class, () -> rounds.save(unknown));
        assertEquals(0L, count("select count(*) from game_sessions where completion_key = ?1", unknown.completionKey()));

        CompletedRoundStore.Round multipleSolo = new CompletedRoundStore.Round(UUID.randomUUID(),
                CompletedRoundStore.State.COMPLETED, game.id, game.currentVersionId,
                null, "SINGLEPLAYER", now, now.plusSeconds(10), List.of(), List.of(firstResult, secondResult));
        assertThrows(IllegalArgumentException.class, () -> rounds.save(multipleSolo));
        CompletedRoundStore.Round hostedSolo = new CompletedRoundStore.Round(UUID.randomUUID(),
                CompletedRoundStore.State.COMPLETED, game.id, game.currentVersionId,
                second.id, "SINGLEPLAYER", now, now.plusSeconds(10), List.of(), List.of(firstResult));
        assertThrows(IllegalArgumentException.class, () -> rounds.save(hostedSolo));
        CompletedRoundStore.Round teamSolo = new CompletedRoundStore.Round(UUID.randomUUID(),
                CompletedRoundStore.State.COMPLETED, game.id, game.currentVersionId,
                null, "SINGLEPLAYER", now, now.plusSeconds(10), List.of(new CompletedRoundStore.Team("A", 10, 1)),
                List.of(new CompletedRoundStore.Participant(first.id, "A", 10, 1, now, List.of())));
        assertThrows(IllegalArgumentException.class, () -> rounds.save(teamSolo));
        assertEquals(0L, count("select count(*) from game_sessions where completion_key = ?1", multipleSolo.completionKey()));
        assertEquals(0L, count("select count(*) from game_sessions where completion_key = ?1", hostedSolo.completionKey()));
        assertEquals(0L, count("select count(*) from game_sessions where completion_key = ?1", teamSolo.completionKey()));

        CompletedRoundStore.Round multiplayer = new CompletedRoundStore.Round(UUID.randomUUID(),
                CompletedRoundStore.State.COMPLETED, game.id, game.currentVersionId,
                null, "MULTIPLAYER", now, now.plusSeconds(10), List.of(), List.of(firstResult, secondResult));
        UUID session = rounds.save(multiplayer);
        assertEquals(2L, count("select count(*) from session_participants where session_id = ?1", session));
    }

    @Test
    void competitionRankingKeepsTiedFirstPlacesAndThirdPlace() {
        Game game = games.create(input("ranking-" + UUID.randomUUID()), null);
        AppUser a = user("rankA");
        AppUser b = user("rankB");
        AppUser c = user("rankC");
        Instant now = Instant.parse("2026-11-03T12:00:00Z");
        CompletedRoundStore.Round result = new CompletedRoundStore.Round(
                UUID.randomUUID(), CompletedRoundStore.State.COMPLETED,
                game.id, game.currentVersionId, null, "MULTIPLAYER", now, now.plusSeconds(30),
                List.of(new CompletedRoundStore.Team("A", 10, 1),
                        new CompletedRoundStore.Team("B", 10, 1),
                        new CompletedRoundStore.Team("C", 5, 3)),
                List.of(new CompletedRoundStore.Participant(a.id, "A", 100, 1, now, List.of()),
                        new CompletedRoundStore.Participant(b.id, "B", 90, 1, now, List.of()),
                        new CompletedRoundStore.Participant(c.id, "C", 20, 3, now, List.of())));
        UUID session = rounds.save(result);
        assertEquals(3L, count("select count(*) from teams where session_id = ?1", session));
        assertEquals(2L, count("select count(*) from teams where session_id = ?1 and placement = 1", session));
        assertEquals(1L, count("select count(*) from teams where session_id = ?1 and placement = 3", session));
        assertEquals(java.util.Set.of(a.id, b.id), java.util.Set.copyOf(rounds.winnerUsers(session)));
        assertEquals(100L, rounds.totalPoints(a.id));
        assertEquals(90L, rounds.totalPoints(b.id));
        assertEquals(20L, rounds.totalPoints(c.id));
    }

    @Test
    void concurrentRetriesProduceOneResultAndDatabaseFailureRollsBack() throws Exception {
        Game game = games.create(input("race-" + UUID.randomUUID()), null);
        AppUser player = user("raceplayer");
        UUID questionVersion = versionOfFirstQuestion(game.currentVersionId);
        Instant now = Instant.parse("2026-11-03T12:00:00Z");
        CompletedRoundStore.Round round = new CompletedRoundStore.Round(
                UUID.randomUUID(), CompletedRoundStore.State.COMPLETED,
                game.id, game.currentVersionId, null, "SINGLEPLAYER", now, now.plusSeconds(30), List.of(),
                List.of(new CompletedRoundStore.Participant(player.id, null, 25, 1, now,
                        List.of(new CompletedRoundStore.Answer(questionVersion, "ja", true, now.plusSeconds(10))))));
        CountDownLatch start = new CountDownLatch(1);
        try (var pool = Executors.newFixedThreadPool(2)) {
            Future<UUID> first = pool.submit(() -> { start.await(); return rounds.save(round); });
            Future<UUID> second = pool.submit(() -> { start.await(); return rounds.save(round); });
            start.countDown();
            assertEquals(first.get(), second.get());
        }
        assertEquals(1L, count("select count(*) from game_sessions where completion_key = ?1", round.completionKey()));
        assertEquals(25L, rounds.totalPoints(player.id));

        String oversized = "x".repeat(4001);
        CompletedRoundStore.Round broken = new CompletedRoundStore.Round(
                UUID.randomUUID(), CompletedRoundStore.State.COMPLETED,
                game.id, game.currentVersionId, null, "SINGLEPLAYER", now, now.plusSeconds(30), List.of(),
                List.of(new CompletedRoundStore.Participant(player.id, null, 25, 1, now,
                        List.of(new CompletedRoundStore.Answer(questionVersion, oversized, true, now.plusSeconds(10))))));
        assertThrows(RuntimeException.class, () -> rounds.save(broken));
        assertEquals(0L, count("select count(*) from game_sessions where completion_key = ?1", broken.completionKey()));
        assertEquals(25L, rounds.totalPoints(player.id));
    }

    private Game input(String id) {
        Game game = new Game();
        game.id = id;
        game.gameType = "Quizbattle";
        game.teacher = "Demo";
        game.name = "Testspiel";
        game.subject = "Programmieren";
        game.tags = List.of("Programmieren", "Grundlagen");
        game.branches = List.of("DDP", "SSE");
        Question question = new Question();
        question.type = "single_choice";
        question.text = "Alte Frage?";
        AnswerOption option = new AnswerOption();
        option.text = "Richtig";
        option.correct = true;
        question.answerOptions = new ArrayList<>(List.of(option));
        game.questions = new ArrayList<>(List.of(question));
        return game;
    }

    private AppUser user(String suffix) {
        AppUser user = new AppUser();
        UUID unique = UUID.randomUUID();
        user.active = true;
        user.createdAt = Instant.now();
        user.updatedAt = user.createdAt;
        user.username = suffix + unique.toString().substring(0, 8);
        user.displayName = suffix;
        user.externalSubject = "test-" + unique;
        user.role = UserRole.TEACHER;
        QuarkusTransaction.requiringNew().run(() -> em.persist(user));
        return user;
    }

    private UUID versionOfFirstQuestion(UUID gameVersion) {
        return asUuid(QuarkusTransaction.requiringNew().call(() -> em.createNativeQuery(
                "select question_version_id from game_version_questions where game_version_id = ?1 and position = 0")
                .setParameter(1, gameVersion).getSingleResult()));
    }

    private long count(String sql, Object argument) {
        return QuarkusTransaction.requiringNew().call(() -> ((Number) em.createNativeQuery(sql)
                .setParameter(1, argument).getSingleResult()).longValue());
    }

    private String scalarString(String sql, Object argument) {
        return QuarkusTransaction.requiringNew().call(() -> (String) em.createNativeQuery(sql)
                .setParameter(1, argument).getSingleResult());
    }

    private List<String> strings(String sql, Object argument) {
        return QuarkusTransaction.requiringNew().call(() -> em.createNativeQuery(sql, String.class)
                .setParameter(1, argument).getResultList());
    }

    private UUID asUuid(Object value) {
        if (value instanceof UUID uuid) return uuid;
        java.nio.ByteBuffer buffer = java.nio.ByteBuffer.wrap((byte[]) value);
        return new UUID(buffer.getLong(), buffer.getLong());
    }
}
