package at.htlleonding.gamebasedlearning.users;

import java.util.Set;

public enum UserRole {
    STUDENT,
    TEACHER,
    ADMIN;

    private static final Set<String> ADMIN_USERNAMES = Set.of("it220269", "it220240", "it220265");

    public static boolean isAdminUsername(String username) {
        return username != null && ADMIN_USERNAMES.contains(username);
    }

    public static UserRole from(String value) {
        if (value == null || value.isBlank()) {
            return STUDENT;
        }

        try {
            return UserRole.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ignored) {
            return STUDENT;
        }
    }
}
