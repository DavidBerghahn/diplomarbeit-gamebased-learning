package at.htlleonding.gamebasedlearning.auth;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.HttpHeaders;

@ApplicationScoped
public class AuthProvider {
    @Inject
    KeycloakAuthProvider keycloakAuthProvider;

    public AuthenticatedUser currentUser(HttpHeaders headers) {
        return keycloakAuthProvider.currentUser(headers);
    }
}
