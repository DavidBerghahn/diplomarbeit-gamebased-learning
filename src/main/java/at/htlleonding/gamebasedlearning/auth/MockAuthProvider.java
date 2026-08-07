package at.htlleonding.gamebasedlearning.auth;

import at.htlleonding.gamebasedlearning.users.UserRole;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.HttpHeaders;

@ApplicationScoped
public class MockAuthProvider implements AuthProvider {
    private static final String DEFAULT_USERNAME = "it220269";
    private static final String DEFAULT_DISPLAY_NAME = "David Berghahn";
    private static final String DEFAULT_CLASS = "5BHITM";

    @Override
    public AuthenticatedUser currentUser(HttpHeaders headers) {
        String username = header(headers, "X-Mock-User", DEFAULT_USERNAME);
        String displayName = header(headers, "X-Mock-Name", DEFAULT_DISPLAY_NAME);
        String schoolClass = header(headers, "X-Mock-Class", DEFAULT_CLASS);
        UserRole role = UserRole.from(header(headers, "X-Mock-Role", UserRole.STUDENT.name()));

        return new AuthenticatedUser(
                "mock:" + username,
                username,
                displayName,
                schoolClass,
                role
        );
    }

    private String header(HttpHeaders headers, String name, String fallback) {
        String value = headers.getHeaderString(name);
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }
}
