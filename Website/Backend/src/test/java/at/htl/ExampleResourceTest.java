package at.htl;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

@QuarkusTest
public class ExampleResourceTest {

    @Test
    public void testGamesEndpoint() {
        given()
                .when().get("/api/games")
                .then()
                .statusCode(200)
                .body("$", hasSize(greaterThan(0)));
    }

}
