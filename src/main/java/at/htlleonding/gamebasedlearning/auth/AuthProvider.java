package at.htlleonding.gamebasedlearning.auth;

import jakarta.ws.rs.core.HttpHeaders;

public interface AuthProvider {
    AuthenticatedUser currentUser(HttpHeaders headers);
}
