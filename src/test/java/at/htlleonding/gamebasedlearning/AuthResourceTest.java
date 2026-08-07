package at.htlleonding.gamebasedlearning;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
class AuthResourceTest {
    @Test
    void returnsDefaultMockUser() {
        given()
                .when().get("/api/auth/me")
                .then()
                .statusCode(200)
                .body("id", notNullValue())
                .body("username", equalTo("it220269"))
                .body("role", equalTo("STUDENT"));
    }

    @Test
    void acceptsMockHeadersForOtherRoles() {
        given()
                .header("X-Mock-User", "teacher01")
                .header("X-Mock-Name", "Test Teacher")
                .header("X-Mock-Role", "TEACHER")
                .when().get("/api/auth/me")
                .then()
                .statusCode(200)
                .body("username", equalTo("teacher01"))
                .body("displayName", equalTo("Test Teacher"))
                .body("role", equalTo("TEACHER"));
    }
}
