package at.htlleonding.gamebasedlearning.games;

import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;
import at.htlleonding.gamebasedlearning.users.AppUser;
import at.htlleonding.gamebasedlearning.users.UserRole;

final class GameAccess {
    static boolean publicGame(Game game) { return "PUBLIC".equals(game.visibility); }
    static boolean owns(Game game, AppUser user) {
        return user != null && user.active && (user.role == UserRole.ADMIN
                || game.createdBy != null && game.createdBy.id.equals(user.id));
    }
    static void read(Game game, AppUser user) {
        if (!publicGame(game) && !owns(game, user)) throw new NotFoundException();
    }
    static void write(Game game, AppUser user) {
        if (!owns(game, user)) throw new ForbiddenException("Nur eigene Spiele können verändert werden.");
    }
}
