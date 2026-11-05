package at.htlleonding.gamebasedlearning;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
class GameResourceTest {
    @jakarta.inject.Inject
    at.htlleonding.gamebasedlearning.games.GameRepository games;
    @jakarta.inject.Inject
    at.htlleonding.gamebasedlearning.games.Smart10SnapshotLoader smart10;

    @Test
    void completeSamplesArePlayableAndLegacyContentIsPreserved() {
        var legacy=games.findById("quizbattle-001").orElseThrow();
        org.junit.jupiter.api.Assertions.assertFalse(legacy.playable());
        org.junit.jupiter.api.Assertions.assertEquals("single_choice",legacy.questions.getFirst().type);
        org.junit.jupiter.api.Assertions.assertEquals(2,legacy.questions.getFirst().answerOptions.size());
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,()->smart10.load(legacy.id));
        var sample=games.findById("sample-smart10-programming").orElseThrow();
        org.junit.jupiter.api.Assertions.assertTrue(sample.playable());
        org.junit.jupiter.api.Assertions.assertEquals(12,smart10.load(sample.id).questions().getFirst().cards().size());
        var justOne=games.findById("sample-just-one-programming").orElseThrow();
        org.junit.jupiter.api.Assertions.assertTrue(justOne.playable());
        org.junit.jupiter.api.Assertions.assertEquals(5,justOne.questions.getFirst().answerOptions.size());
        given().get("/api/games/{id}",legacy.id).then().statusCode(200)
                .body("spielbar",org.hamcrest.Matchers.equalTo(false))
                .body("startHinweis",notNullValue());
    }
    @Test
    void listsSeededGames() {
        given()
                .when().get("/api/games")
                .then()
                .statusCode(200)
                .body("size()", greaterThanOrEqualTo(2))
                .body("[0].id", notNullValue());
    }

    @Test
    void groupsGamesByType() {
        given()
                .when().get("/api/games/grouped")
                .then()
                .statusCode(200)
                .body("spiele", notNullValue());
    }
}
