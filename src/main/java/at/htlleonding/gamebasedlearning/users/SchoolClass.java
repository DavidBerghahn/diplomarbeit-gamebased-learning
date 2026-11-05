package at.htlleonding.gamebasedlearning.users;

import jakarta.ws.rs.BadRequestException;
import java.util.Locale;

/** Canonical class names keep identity claims and manually supplied names in the same group. */
public final class SchoolClass {
    private SchoolClass() { }

    public static String normalize(String value) {
        if (value == null || value.isBlank()) return null;
        String normalized = value.strip().toUpperCase(Locale.ROOT);
        if (normalized.length() > 40 || normalized.chars().anyMatch(Character::isISOControl)) {
            throw new BadRequestException("Die Klasse darf höchstens 40 Zeichen und keine Steuerzeichen enthalten.");
        }
        return normalized;
    }
}
