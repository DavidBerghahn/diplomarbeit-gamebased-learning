package at.htlleonding.gamebasedlearning.users;

import java.time.Instant;
import java.util.UUID;

public record UserProfileResponse(
        UUID id,
        String externalSubject,
        String username,
        String displayName,
        String schoolClass,
        UserRole role,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
    public static UserProfileResponse from(AppUser user) {
        return new UserProfileResponse(
                user.id,
                user.externalSubject,
                user.username,
                user.displayName,
                user.schoolClass,
                user.role,
                user.active,
                user.createdAt,
                user.updatedAt
        );
    }
}
