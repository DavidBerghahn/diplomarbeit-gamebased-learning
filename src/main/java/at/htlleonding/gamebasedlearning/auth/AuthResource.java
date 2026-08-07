package at.htlleonding.gamebasedlearning.auth;

import at.htlleonding.gamebasedlearning.users.AppUser;
import at.htlleonding.gamebasedlearning.users.UserProfileResponse;
import at.htlleonding.gamebasedlearning.users.UserService;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;

@Path("/api/auth")
@Produces(MediaType.APPLICATION_JSON)
public class AuthResource {
    @Inject
    AuthProvider authProvider;

    @Inject
    UserService userService;

    @GET
    @Path("/me")
    public UserProfileResponse me(@Context HttpHeaders headers) {
        AuthenticatedUser identity = authProvider.currentUser(headers);
        AppUser user = userService.getOrCreateFromIdentity(identity);
        return UserProfileResponse.from(user);
    }
}
