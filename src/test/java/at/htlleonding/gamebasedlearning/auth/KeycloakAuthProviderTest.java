package at.htlleonding.gamebasedlearning.auth;

import at.htlleonding.gamebasedlearning.users.UserRole;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KeycloakAuthProviderTest {
    @Test
    void recognizesTeacherOrganizationalUnit() {
        String distinguishedName = "CN=teacher,OU=Teachers,OU=HTL,DC=EDU,DC=HTL-LEONDING,DC=AC,DC=AT";

        assertTrue(KeycloakAuthProvider.isTeacherDistinguishedName(distinguishedName));
    }

    @Test
    void doesNotTreatStudentAsTeacher() {
        String distinguishedName = "CN=student,OU=5BHITM,OU=Students,OU=HTL,DC=EDU,DC=HTL-LEONDING,DC=AC,DC=AT";

        assertFalse(KeycloakAuthProvider.isTeacherDistinguishedName(distinguishedName));
    }

    @Test
    void rejectsMalformedDistinguishedName() {
        assertFalse(KeycloakAuthProvider.isTeacherDistinguishedName("not-an-ldap-name,"));
    }

    @Test
    void grantsAdminOnlyToTheThreeExactPreferredUsernames() {
        for (String username : Set.of("it220269", "it220240", "it220265")) {
            assertEquals(UserRole.ADMIN, roleFor(Map.of("preferred_username", username)));
        }
        assertEquals(UserRole.STUDENT, roleFor(Map.of("preferred_username", "student")));
        assertEquals(UserRole.STUDENT, roleFor(Map.of("preferred_username", "IT220269")));
        assertEquals(UserRole.STUDENT, roleFor(Map.of("preferred_username", " it220269 ")));
        assertEquals(UserRole.STUDENT, roleFor(Map.of("username", "it220269")));
    }

    @Test
    void ignoresAdminGroupsAndClaimsForNonAllowlistedAccounts() {
        assertEquals(UserRole.STUDENT, roleFor(
                Map.of("preferred_username", "student", "realm_access", Map.of("roles", Set.of("admin"))),
                Set.of("admins")
        ));
        assertEquals(UserRole.STUDENT, roleFor(Map.of(
                "preferred_username", "student", "custom_roles", Set.of("administrator"),
                "resource_access", Map.of("frontend", Map.of("roles", Set.of("admin")))
        )));
    }

    @Test
    void preservesTeacherRecognitionWithoutGivingAdmin() {
        assertEquals(UserRole.TEACHER, roleFor(Map.of(
                "preferred_username", "teacher",
                "distinguishedName", "CN=teacher,OU=Teachers,OU=HTL,DC=EDU,DC=HTL-LEONDING,DC=AC,DC=AT"
        )));
        assertEquals(UserRole.TEACHER, roleFor(Map.of(
                "preferred_username", "teacher", "custom_roles", Set.of("teacher")
        )));
    }

    @Test
    void configuredDefaultCannotGrantAdmin() {
        KeycloakAuthProvider provider = new KeycloakAuthProvider(jwt(Map.of("preferred_username", "student"), Set.of()), "frontend", "ADMIN");

        assertEquals(UserRole.STUDENT, provider.currentUser(null).role());
    }

    private UserRole roleFor(Map<String, Object> claims) {
        return roleFor(claims, Set.of());
    }

    private UserRole roleFor(Map<String, Object> claims, Set<String> groups) {
        return new KeycloakAuthProvider(jwt(claims, groups), "frontend", "STUDENT")
                .currentUser(null).role();
    }

    private JsonWebToken jwt(Map<String, Object> claims, Set<String> groups) {
        return (JsonWebToken) Proxy.newProxyInstance(
                JsonWebToken.class.getClassLoader(),
                new Class<?>[]{JsonWebToken.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getSubject" -> "subject";
                    case "getClaim" -> claims.get(args[0]);
                    case "getGroups" -> groups;
                    case "getClaimNames" -> claims.keySet();
                    case "getName" -> "subject";
                    default -> null;
                }
        );
    }
}
