package at.htlleonding.gamebasedlearning.users;

import at.htlleonding.gamebasedlearning.auth.AuthProvider;
import at.htlleonding.gamebasedlearning.auth.AuthenticatedUser;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.core.HttpHeaders;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
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
                () -> resource.updateUser(null, userId, new UpdateUserRequest())
        );

        assertAll(requests.stream()
                .map(request -> (Executable) () -> assertThrows(ForbiddenException.class, request)));
    }

    private UserResource resourceFor(AppUser user) {
        UserResource resource = new UserResource();
        resource.authProvider = new StubAuthProvider();
        resource.userService = new StubUserService(user);
        return resource;
    }

    private AppUser inactiveAdmin() {
        AppUser user = new AppUser();
        user.id = UUID.randomUUID();
        user.role = UserRole.ADMIN;
        user.active = false;
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
    }
}
