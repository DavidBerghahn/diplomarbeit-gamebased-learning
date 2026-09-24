package at.htlleonding.gamebasedlearning.users;

import at.htlleonding.gamebasedlearning.auth.AuthenticatedUser;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

@QuarkusTest
class UserServiceRoleTest {
    @Inject
    UserService userService;

    @Test
    @TestTransaction
    void upgradesExistingAllowlistedAccountAndDropsOldAdminForOtherAccount() {
        for (String username : Set.of("it220269", "it220240", "it220265")) {
            AppUser allowed = storedUser(username, UserRole.STUDENT);
            assertEquals(UserRole.ADMIN, userService.getOrCreateFromIdentity(identity(allowed, UserRole.ADMIN)).role);
        }
        AppUser other = storedUser("student", UserRole.ADMIN);

        assertEquals(UserRole.STUDENT, userService.getOrCreateFromIdentity(identity(other, UserRole.STUDENT)).role);
    }

    @Test
    @TestTransaction
    void refusesAdminIdentityForNonAllowlistedAccount() {
        AppUser user = userService.getOrCreateFromIdentity(
                new AuthenticatedUser("keycloak:" + UUID.randomUUID(), "student", "Student", null, UserRole.ADMIN)
        );

        assertEquals(UserRole.STUDENT, user.role);
    }

    @Test
    @TestTransaction
    void retainsTeacherRecognitionWhileRemovingAnOldAdminRole() {
        AppUser formerAdmin = storedUser("teacher", UserRole.ADMIN);
        AppUser teacher = storedUser("another-teacher", UserRole.TEACHER);

        assertEquals(UserRole.TEACHER, userService.getOrCreateFromIdentity(identity(formerAdmin, UserRole.TEACHER)).role);
        assertEquals(UserRole.TEACHER, userService.getOrCreateFromIdentity(identity(teacher, UserRole.STUDENT)).role);
    }

    @Test
    @TestTransaction
    void rejectsRoleGrantAndRevocationButKeepsOtherPatchFields() {
        AppUser allowed = storedUser("it220240", UserRole.ADMIN);
        AppUser other = storedUser("student", UserRole.STUDENT);
        UpdateUserRequest grant = new UpdateUserRequest();
        grant.role = UserRole.ADMIN;
        grant.displayName = "Unexpected";
        UpdateUserRequest revoke = new UpdateUserRequest();
        revoke.role = UserRole.STUDENT;

        assertThrows(BadRequestException.class, () -> userService.updateUser(other.id, grant));
        assertThrows(BadRequestException.class, () -> userService.updateUser(allowed.id, revoke));
        assertEquals(UserRole.STUDENT, other.role);
        assertEquals("Student", other.displayName);
        assertEquals(UserRole.ADMIN, allowed.role);

        UpdateUserRequest profileChange = new UpdateUserRequest();
        profileChange.displayName = "New name";
        profileChange.active = false;
        AppUser updated = userService.updateUser(other.id, profileChange);
        assertEquals("New name", updated.displayName);
        assertFalse(updated.active);
        assertEquals(UserRole.STUDENT, updated.role);
    }

    private AppUser storedUser(String username, UserRole role) {
        AppUser user = new AppUser();
        user.externalSubject = "keycloak:" + UUID.randomUUID();
        user.username = username;
        user.displayName = "Student";
        user.role = role;
        user.persist();
        return user;
    }

    private AuthenticatedUser identity(AppUser user, UserRole role) {
        return new AuthenticatedUser(user.externalSubject, user.username, user.displayName, null, role);
    }
}
