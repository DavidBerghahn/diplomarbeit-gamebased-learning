package at.htlleonding.gamebasedlearning.games;

import at.htlleonding.gamebasedlearning.auth.AuthProvider;
import at.htlleonding.gamebasedlearning.auth.AuthenticatedUser;
import at.htlleonding.gamebasedlearning.users.AppUser;
import at.htlleonding.gamebasedlearning.users.UserRole;
import at.htlleonding.gamebasedlearning.users.UserService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.junit.QuarkusMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.ws.rs.NotAuthorizedException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class JustOnePublicReadTest {
    @Inject GameRepository games;
    @Inject GameSocket socket;
    @Inject UserService users;
    @Inject EntityManager em;
    @Inject ObjectMapper mapper;

    @Test
    void publicHttpAndSocketViewsHideTargetsWhileEditorEnforcesOwnership() throws Exception {
        AtomicReference<AuthenticatedUser> identity = new AtomicReference<>();
        QuarkusMock.installMockForType(new AuthProvider() {
            @Override public AuthenticatedUser currentUser(jakarta.ws.rs.core.HttpHeaders headers) {
                if (identity.get() == null) throw new NotAuthorizedException("Bearer");
                return identity.get();
            }
        }, AuthProvider.class);
        AuthenticatedUser ownerIdentity = teacher("owner");
        identity.set(ownerIdentity);
        AppUser owner = users.getOrCreateFromIdentity(identity.get());
        String secret = "UnverwechselbarZielwort" + UUID.randomUUID().toString().replace("-", "");
        Game game = new Game();
        game.id = "just-one-public-" + UUID.randomUUID();
        game.gameType = "JustOne";
        game.name = "Geschütztes Just One";
        game.visibility = "PUBLIC";
        Question question = new Question();
        question.type = "JUST_ONE";
        question.text = "Kategorie";
        for (String word : List.of(secret, "Zwei", "Drei", "Vier", "Fünf")) {
            AnswerOption option = new AnswerOption();
            option.text = word;
            question.answerOptions.add(option);
        }
        game.questions.add(question);
        Game saved = games.create(game, owner);
        UUID version = saved.currentVersionId;

        identity.set(null);
        for (String path : List.of("/api/games", "/api/games/grouped", "/api/games/type/JustOne",
                "/api/games/" + saved.id)) {
            String body = io.restassured.RestAssured.given().get(path).then().statusCode(200)
                    .extract().asString();
            assertFalse(body.contains(secret), path);
            if (path.endsWith(saved.id)) {
                JsonNode detail = mapper.readTree(body);
                assertTrue(detail.path("spielbar").asBoolean());
                assertTrue(detail.path("startHinweis").isNull());
                assertEquals(0, detail.path("fragen").get(0).path("antwortmoeglichkeiten").size());
            }
        }
        for (String command : List.of("get_games", "get_games_by_type", "get_game")) {
            String payload = mapper.writeValueAsString(java.util.Map.of("type", command,
                    "gameType", "JustOne", "id", saved.id));
            assertFalse(socket.onMessage(payload, null).contains(secret), command);
        }
        assertEquals(version, games.findById(saved.id).orElseThrow().currentVersionId);
        assertEquals(secret, games.findById(saved.id).orElseThrow()
                .questions.getFirst().answerOptions.getFirst().text);

        String editorPath = "/api/games/" + saved.id + "/editor";
        String libraryPath = "/api/games/" + saved.id + "/library";
        io.restassured.RestAssured.given().get(editorPath).then().statusCode(401);
        io.restassured.RestAssured.given().get(libraryPath).then().statusCode(401);
        AuthenticatedUser otherTeacher = teacher("other");
        identity.set(otherTeacher);
        io.restassured.RestAssured.given().get(editorPath).then().statusCode(403);
        int gameCount = games.findAll().size();
        String imported = io.restassured.RestAssured.given().get(libraryPath).then().statusCode(200)
                .extract().asString();
        assertTrue(imported.contains(secret));
        assertEquals("JustOne", mapper.readTree(imported).path("spiel_typ").asText());
        assertEquals(gameCount, games.findAll().size(), "Reading the library must not create a copy");
        identity.set(new AuthenticatedUser("student-" + UUID.randomUUID(), "student", "Student", null,
                UserRole.STUDENT));
        io.restassured.RestAssured.given().get(editorPath).then().statusCode(403);
        io.restassured.RestAssured.given().get(libraryPath).then().statusCode(403);
        identity.set(ownerIdentity);
        assertTrue(io.restassured.RestAssured.given().get(libraryPath).then().statusCode(200)
                .extract().asString().contains(secret));
        String editable = io.restassured.RestAssured.given().get(editorPath).then().statusCode(200)
                .extract().asString();
        assertTrue(editable.contains(secret));
        com.fasterxml.jackson.databind.node.ObjectNode edited = (com.fasterxml.jackson.databind.node.ObjectNode)
                mapper.readTree(editable);
        edited.put("name", "Bearbeitetes Just One");
        io.restassured.RestAssured.given().contentType("application/json").body(edited.toString())
                .put("/api/games/{id}", saved.id).then().statusCode(200);
        em.clear();
        assertEquals("Bearbeitetes Just One", games.findById(saved.id).orElseThrow().name);
        assertEquals(secret, games.findById(saved.id).orElseThrow()
                .questions.getFirst().answerOptions.getFirst().text);
        assertFalse(io.restassured.RestAssured.given().get("/api/games/{id}", saved.id).then().statusCode(200)
                .extract().asString().contains(secret));
        identity.set(new AuthenticatedUser("admin-" + UUID.randomUUID(), "it220269", "Admin", null,
                UserRole.ADMIN));
        assertTrue(io.restassured.RestAssured.given().get(editorPath).then().statusCode(200)
                .extract().asString().contains(secret));
        assertTrue(io.restassured.RestAssured.given().get(libraryPath).then().statusCode(200)
                .extract().asString().contains(secret));

        identity.set(teacher("copier"));
        String copied = io.restassured.RestAssured.given().contentType("application/json").body("{}")
                .post("/api/games/{id}/copy", saved.id)
                .then().statusCode(200).extract().asString();
        String copyId = mapper.readTree(copied).path("id").asText();
        assertNotEquals(saved.id, copyId);
        assertTrue(io.restassured.RestAssured.given().get("/api/games/{id}/editor", copyId)
                .then().statusCode(200).extract().asString().contains(secret));
        assertFalse(io.restassured.RestAssured.given().get("/api/games/{id}", copyId)
                .then().statusCode(200).extract().asString().contains(secret));

        io.quarkus.narayana.jta.QuarkusTransaction.requiringNew().run(() ->
                em.find(Game.class, saved.id).visibility = "PRIVATE");
        em.clear();
        identity.set(otherTeacher);
        io.restassured.RestAssured.given().get(libraryPath).then().statusCode(404);
        io.restassured.RestAssured.given().get(editorPath).then().statusCode(403);
        identity.set(ownerIdentity);
        assertTrue(io.restassured.RestAssured.given().get(libraryPath).then().statusCode(200)
                .extract().asString().contains(secret));
    }

    private AuthenticatedUser teacher(String suffix) {
        return new AuthenticatedUser("subject-" + suffix + "-" + UUID.randomUUID(), suffix,
                suffix, null, UserRole.TEACHER);
    }
}
