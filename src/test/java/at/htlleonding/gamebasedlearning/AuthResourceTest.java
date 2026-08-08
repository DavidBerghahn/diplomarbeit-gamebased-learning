package at.htlleonding.gamebasedlearning;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

@QuarkusTest
class AuthResourceTest {
    @Test
    void rejectsMissingBearerToken() {
        given()
                .when().get("/api/auth/me")
                .then()
                .statusCode(401);
    }

    @Test
    void exposesAuthConfig() {
        given()
                .when().get("/api/auth/config")
                .then()
                .statusCode(200)
                .body("provider", equalTo("keycloak"))
                .body("keycloakUrl", equalTo("https://auth.htl-leonding.ac.at"))
                .body("keycloakRealm", equalTo("2526_5bhitm"))
                .body("keycloakClientId", equalTo("frontend"));
    }
}
