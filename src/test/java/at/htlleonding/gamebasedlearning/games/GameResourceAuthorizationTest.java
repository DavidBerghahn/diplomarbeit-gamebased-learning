package at.htlleonding.gamebasedlearning.games;

import at.htlleonding.gamebasedlearning.auth.AuthProvider;
import at.htlleonding.gamebasedlearning.auth.AuthenticatedUser;
import at.htlleonding.gamebasedlearning.users.AppUser;
import at.htlleonding.gamebasedlearning.users.UserRole;
import at.htlleonding.gamebasedlearning.users.UserService;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.core.HttpHeaders;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GameResourceAuthorizationTest {
    @Test
    void inactiveTeachersCannotModifyGames() {
        GameResource resource = resourceFor(user(UserRole.TEACHER, false));

        assertAll(
                () -> assertThrows(ForbiddenException.class, () -> resource.create(null, new Game())),
                () -> assertThrows(ForbiddenException.class, () -> resource.update(null, "game", new Game())),
                () -> assertThrows(ForbiddenException.class, () -> resource.delete(null, "game"))
        );
    }

    private GameResource resourceFor(AppUser user) {
        GameResource resource = new GameResource();
        resource.authProvider = new StubAuthProvider();
        resource.userService = new StubUserService(user);
        return resource;
    }

    private AppUser user(UserRole role, boolean active) {
        AppUser user = new AppUser();
        user.role = role;
        user.active = active;
        return user;
    }

    private static class StubAuthProvider extends AuthProvider {
        @Override
        public AuthenticatedUser currentUser(HttpHeaders headers) {
            return new AuthenticatedUser("subject", "user", "User", null, UserRole.TEACHER);
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
