package at.htlleonding.gamebasedlearning.auth;

import at.htlleonding.gamebasedlearning.users.UserRole;

public record AuthenticatedUser(
        String externalSubject,
        String username,
        String displayName,
        String schoolClass,
        UserRole role
) {
}
