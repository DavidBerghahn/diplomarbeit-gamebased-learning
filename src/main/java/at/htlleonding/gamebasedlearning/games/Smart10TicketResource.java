package at.htlleonding.gamebasedlearning.games;

import at.htlleonding.gamebasedlearning.auth.AuthProvider;
import at.htlleonding.gamebasedlearning.users.AppUser;
import at.htlleonding.gamebasedlearning.users.UserService;
import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;

import java.util.Map;

@Path("/api/smart10/ws-ticket")
public class Smart10TicketResource {
    @Inject AuthProvider auth;
    @Inject UserService users;
    @Inject Smart10Tickets tickets;

    @POST
    public Map<String, String> issue(@Context HttpHeaders headers) {
        AppUser user = users.getOrCreateFromIdentity(auth.currentUser(headers));
        return Map.of("ticket", tickets.issue(user));
    }
}
