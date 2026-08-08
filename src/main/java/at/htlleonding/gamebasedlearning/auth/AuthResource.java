package at.htlleonding.gamebasedlearning.auth;

import at.htlleonding.gamebasedlearning.users.AppUser;
import at.htlleonding.gamebasedlearning.users.UserProfileResponse;
import at.htlleonding.gamebasedlearning.users.UserService;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Path("/api/auth")
@Produces(MediaType.APPLICATION_JSON)
public class AuthResource {
    @ConfigProperty(name = "quarkus.oidc.auth-server-url", defaultValue = "https://auth.htl-leonding.ac.at/realms/2526_5bhitm")
    String oidcAuthServerUrl;

    @ConfigProperty(name = "quarkus.oidc.client-id", defaultValue = "frontend")
    String oidcClientId;

    @Inject
    AuthProvider authProvider;

    @Inject
    UserService userService;

    @GET
    @Path("/config")
    public AuthConfigResponse config() {
        return new AuthConfigResponse(
                "keycloak",
                keycloakBaseUrl(),
                keycloakRealm(),
                oidcClientId
        );
    }

    @GET
    @Path("/me")
    public UserProfileResponse me(@Context HttpHeaders headers) {
        AuthenticatedUser identity = authProvider.currentUser(headers);
        AppUser user = userService.getOrCreateFromIdentity(identity);
        return UserProfileResponse.from(user);
    }

    private String keycloakBaseUrl() {
        int realmIndex = oidcAuthServerUrl.indexOf("/realms/");
        if (realmIndex < 0) {
            return oidcAuthServerUrl;
        }
        return oidcAuthServerUrl.substring(0, realmIndex);
    }

    private String keycloakRealm() {
        int realmIndex = oidcAuthServerUrl.indexOf("/realms/");
        if (realmIndex < 0) {
            return "";
        }
        return oidcAuthServerUrl.substring(realmIndex + "/realms/".length());
    }
}
