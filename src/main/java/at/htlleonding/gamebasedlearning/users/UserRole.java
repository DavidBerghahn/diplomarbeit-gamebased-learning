package at.htlleonding.gamebasedlearning.users;

public enum UserRole {
    STUDENT,
    TEACHER,
    ADMIN;

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
