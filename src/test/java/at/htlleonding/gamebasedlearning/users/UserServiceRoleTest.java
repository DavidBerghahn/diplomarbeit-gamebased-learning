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

    @Test
    @TestTransaction
    void retainsAssignedClassWithoutClaimAndUpdatesItFromSchoolIdentity() {
        AppUser student = storedUser("class-student", UserRole.STUDENT);
        assertEquals("5BHITM", userService.assignSchoolClass(student.id, " 5bhitm ").schoolClass);
        assertEquals("5BHITM", userService.getOrCreateFromIdentity(identity(student, UserRole.STUDENT)).schoolClass);
        assertEquals("5AHITM", userService.getOrCreateFromIdentity(new AuthenticatedUser(
                student.externalSubject, student.username, student.displayName, " 5ahitm ", UserRole.STUDENT)).schoolClass);

        AppUser newStudent = userService.getOrCreateFromIdentity(new AuthenticatedUser(
                "keycloak:" + UUID.randomUUID(), "new-class-student", "Student", " 4bhitm ", UserRole.STUDENT));
        assertEquals("4BHITM", newStudent.schoolClass);
        AppUser unknown = userService.getOrCreateFromIdentity(new AuthenticatedUser(
                "keycloak:" + UUID.randomUUID(), "unknown-class", "Student", null, UserRole.STUDENT));
        org.junit.jupiter.api.Assertions.assertNull(unknown.schoolClass);
    }

    @Test
    @TestTransaction
    void rejectsEmptyAndInvalidClassAssignmentsAndTeachers() {
        AppUser student = storedUser("class-validation", UserRole.STUDENT);
        AppUser teacher = storedUser("teacher-validation", UserRole.TEACHER);
        for (String value : new String[]{null, " ", "Ohne Klasse", "a".repeat(41), "5B\nHITM"}) {
            assertThrows(BadRequestException.class, () -> userService.assignSchoolClass(student.id, value));
        }
        assertThrows(BadRequestException.class, () -> userService.assignSchoolClass(teacher.id, "5BHITM"));
    }

    @Test
    @TestTransaction
    void canAssignStudentAdminWithoutChangingPermissions() {
        AppUser admin = storedUser("it220240", UserRole.ADMIN);
        assertEquals("5BHITM", userService.assignSchoolClass(admin.id, " 5bhitm ").schoolClass);
        AppUser refreshed = userService.getOrCreateFromIdentity(identity(admin, UserRole.ADMIN));
        assertEquals("5BHITM", refreshed.schoolClass);
        assertEquals(UserRole.ADMIN, refreshed.role);
    }

    private AuthenticatedUser identity(AppUser user, UserRole role) {
        return new AuthenticatedUser(user.externalSubject, user.username, user.displayName, null, role);
    }
}
