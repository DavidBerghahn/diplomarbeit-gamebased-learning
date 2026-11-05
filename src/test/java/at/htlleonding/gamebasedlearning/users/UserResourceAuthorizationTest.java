package at.htlleonding.gamebasedlearning.users;

import at.htlleonding.gamebasedlearning.auth.AuthProvider;
import at.htlleonding.gamebasedlearning.auth.AuthenticatedUser;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.core.HttpHeaders;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UserResourceAuthorizationTest {
    @Test
    void inactiveUsersCannotUseAnyUserEndpoint() {
        UserResource resource = resourceFor(inactiveAdmin());
        UUID userId = UUID.randomUUID();
        List<Executable> requests = List.of(
                () -> resource.ownProfile(null),
                () -> resource.updateOwnProfile(null, new UpdateOwnProfileRequest()),
                () -> resource.listUsers(null),
                () -> resource.getUser(null, userId),
                () -> resource.updateUser(null, userId, new UpdateUserRequest()),
                () -> resource.assignSchoolClass(null, userId, new UserResource.SchoolClassRequest("5BHITM"))
        );

        assertAll(requests.stream()
                .map(request -> (Executable) () -> assertThrows(ForbiddenException.class, request)));
    }

    @Test
    void patchCannotGrantOrRevokeAnAdminRole() {
        AppUser admin = activeAdmin();
        UserResource resource = resourceFor(admin);
        UpdateUserRequest grant = new UpdateUserRequest();
        grant.role = UserRole.ADMIN;
        UpdateUserRequest revoke = new UpdateUserRequest();
        revoke.role = UserRole.STUDENT;

        assertThrows(BadRequestException.class, () -> resource.updateUser(null, admin.id, grant));
        assertThrows(BadRequestException.class, () -> resource.updateUser(null, admin.id, revoke));
        assertEquals(UserRole.ADMIN, admin.role);
    }

    private UserResource resourceFor(AppUser user) {
        UserResource resource = new UserResource();
        resource.authProvider = new StubAuthProvider();
        resource.userService = new StubUserService(user);
        return resource;
    }

    @Test
    void studentsCannotAssignClassesButTeachersAndAdminsCan() {
        AppUser viewer = activeAdmin();
        viewer.role = UserRole.STUDENT;
        UserResource resource = resourceFor(viewer);
        var request = new UserResource.SchoolClassRequest("5BHITM");
        assertThrows(ForbiddenException.class, () -> resource.assignSchoolClass(null, viewer.id, request));
        viewer.role = UserRole.TEACHER;
        assertEquals("5BHITM", resource.assignSchoolClass(null, viewer.id, request).schoolClass());
        viewer.role = UserRole.ADMIN;
        assertEquals("5BHITM", resource.assignSchoolClass(null, viewer.id, request).schoolClass());
        assertThrows(BadRequestException.class, () -> resource.assignSchoolClass(null, viewer.id, null));
    }

    private AppUser inactiveAdmin() {
        AppUser user = activeAdmin();
        user.active = false;
        return user;
    }

    private AppUser activeAdmin() {
        AppUser user = new AppUser();
        user.id = UUID.randomUUID();
        user.role = UserRole.ADMIN;
        user.active = true;
        return user;
    }

    private static class StubAuthProvider extends AuthProvider {
        @Override
        public AuthenticatedUser currentUser(HttpHeaders headers) {
            return new AuthenticatedUser("subject", "admin", "Admin", null, UserRole.ADMIN);
        }
    }

    private static class StubUserService extends UserService {
        private final AppUser user;

        private StubUserService(AppUser user) {
            this.user = user;
        }

        @Override
        public AppUser getOrCreateFromIdentity(AuthenticatedUser identity) {
            return user;
        }

        @Override
        public AppUser getById(UUID id) {
            return user;
        }

        @Override
        public AppUser assignSchoolClass(UUID id, String schoolClass) {
            user.schoolClass = schoolClass;
            return user;
        }
    }
}
