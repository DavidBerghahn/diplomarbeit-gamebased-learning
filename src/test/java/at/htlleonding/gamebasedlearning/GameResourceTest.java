package at.htlleonding.gamebasedlearning;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
class GameResourceTest {
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
