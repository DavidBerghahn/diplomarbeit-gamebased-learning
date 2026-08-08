package at.htlleonding.gamebasedlearning.auth;

import at.htlleonding.gamebasedlearning.users.UserRole;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.core.HttpHeaders;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@ApplicationScoped
public class KeycloakAuthProvider {
    private static final Pattern DISTINGUISHED_NAME_CLASS_PATTERN = Pattern.compile("OU=([1-5][A-Z0-9]{2,12})", Pattern.CASE_INSENSITIVE);

    private final JsonWebToken jwt;
    private final String clientId;
    private final UserRole defaultRole;

    public KeycloakAuthProvider(
            JsonWebToken jwt,
            @ConfigProperty(name = "quarkus.oidc.client-id", defaultValue = "frontend") String clientId,
            @ConfigProperty(name = "auth.keycloak.default-role", defaultValue = "STUDENT") String defaultRole
    ) {
        this.jwt = jwt;
        this.clientId = clientId;
        this.defaultRole = UserRole.from(defaultRole);
    }

    public AuthenticatedUser currentUser(HttpHeaders headers) {
        String subject = trimToNull(jwt.getSubject());
        if (subject == null) {
            throw new NotAuthorizedException("Missing Keycloak bearer token");
        }

        String username = firstClaim("preferred_username", "username", "email");
        if (username == null) {
            username = subject;
        }

        String displayName = firstClaim("name", "display_name", "given_name");
        if (displayName == null) {
            displayName = username;
        }

        String distinguishedName = claimAsString("distinguishedName");
        String schoolClass = firstClaim("school_class", "schoolClass", "class", "klasse");
        if (schoolClass == null) {
            schoolClass = classFromDistinguishedName(distinguishedName);
        }

        return new AuthenticatedUser(
                "keycloak:" + subject,
                username,
                displayName,
                schoolClass,
                roleFromClaims(distinguishedName)
        );
    }

    private String firstClaim(String... claimNames) {
        for (String claimName : claimNames) {
            String value = claimAsString(claimName);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private String claimAsString(String claimName) {
        Object value = jwt.getClaim(claimName);
        if (value == null) {
            return null;
        }
        return trimToNull(String.valueOf(value));
    }

    private String classFromDistinguishedName(String distinguishedName) {
        if (distinguishedName == null) {
            return null;
        }

        Matcher matcher = DISTINGUISHED_NAME_CLASS_PATTERN.matcher(distinguishedName);
        while (matcher.find()) {
            String organizationalUnit = matcher.group(1).toUpperCase(Locale.ROOT);
            if (!"HTL".equals(organizationalUnit)) {
                return organizationalUnit;
            }
        }
        return null;
    }

    private UserRole roleFromClaims(String distinguishedName) {
        Set<String> roleValues = new LinkedHashSet<>();
        addAll(roleValues, jwt.getGroups());
        addClaimValues(roleValues, "custom_roles");
        addClaimValues(roleValues, "groups");
        addNestedRoles(roleValues, "realm_access", "roles");
        addResourceRoles(roleValues);

        if (containsAny(roleValues, "admin", "admins", "administrator")) {
            return UserRole.ADMIN;
        }
        if (containsAny(roleValues, "teacher", "teachers", "lehrer", "professor")) {
            return UserRole.TEACHER;
        }
        if (distinguishedName != null && distinguishedName.toLowerCase(Locale.ROOT).contains("ou=lehrer")) {
            return UserRole.TEACHER;
        }
        return defaultRole;
    }

    private void addResourceRoles(Set<String> target) {
        Object resourceAccess = jwt.getClaim("resource_access");
        if (!(resourceAccess instanceof Map<?, ?> resources)) {
            return;
        }

        Object clientAccess = resources.get(clientId);
        if (!(clientAccess instanceof Map<?, ?> clientAccessMap)) {
            return;
        }
        Object roles = clientAccessMap.get("roles");
        addValues(target, roles);
    }

    private void addNestedRoles(Set<String> target, String claimName, String nestedName) {
        Object claim = jwt.getClaim(claimName);
        if (!(claim instanceof Map<?, ?> claimMap)) {
            return;
        }
        addValues(target, claimMap.get(nestedName));
    }

    private void addClaimValues(Set<String> target, String claimName) {
        addValues(target, jwt.getClaim(claimName));
    }

    private void addValues(Set<String> target, Object value) {
        if (value instanceof Collection<?> values) {
            values.stream()
                    .filter(Objects::nonNull)
                    .map(String::valueOf)
                    .map(this::trimToNull)
                    .filter(Objects::nonNull)
                    .forEach(target::add);
            return;
        }

        String text = trimToNull(value == null ? null : String.valueOf(value));
        if (text != null) {
            target.add(text);
        }
    }

    private void addAll(Set<String> target, Set<String> values) {
        if (values == null) {
            return;
        }
        values.stream()
                .map(this::trimToNull)
                .filter(Objects::nonNull)
                .forEach(target::add);
    }

    private boolean containsAny(Set<String> values, String... expectedValues) {
        Set<String> normalized = new LinkedHashSet<>();
        values.stream()
                .map(value -> value.toLowerCase(Locale.ROOT))
                .forEach(normalized::add);

        for (String expectedValue : expectedValues) {
            if (normalized.contains(expectedValue)) {
                return true;
            }
        }
        return false;
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
