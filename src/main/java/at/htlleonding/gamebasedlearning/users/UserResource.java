package at.htlleonding.gamebasedlearning.users;

import at.htlleonding.gamebasedlearning.auth.AuthProvider;
import at.htlleonding.gamebasedlearning.auth.AuthenticatedUser;
import jakarta.inject.Inject;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;

import java.util.List;
import java.util.UUID;

@Path("/api/users")
@Produces(MediaType.APPLICATION_JSON)
public class UserResource {
    @Inject
    AuthProvider authProvider;

    @Inject
    UserService userService;

    @GET
    @Path("/me")
    public UserProfileResponse ownProfile(@Context HttpHeaders headers) {
        return UserProfileResponse.from(currentUser(headers));
    }

    @PUT
    @Path("/me")
    public UserProfileResponse updateOwnProfile(@Context HttpHeaders headers, UpdateOwnProfileRequest request) {
        AppUser user = currentUser(headers);
        return UserProfileResponse.from(userService.updateOwnProfile(user, request));
    }

    @GET
    public List<UserProfileResponse> listUsers(@Context HttpHeaders headers) {
        AppUser user = currentUser(headers);
        requireAtLeastTeacher(user);
        return userService.listUsers().stream()
                .map(UserProfileResponse::from)
                .toList();
    }

    @GET
    @Path("/{id}")
    public UserProfileResponse getUser(@Context HttpHeaders headers, @PathParam("id") UUID id) {
        AppUser current = currentUser(headers);
        AppUser requested = userService.getById(id);
        if (!current.id.equals(requested.id)) {
            requireAtLeastTeacher(current);
        }
        return UserProfileResponse.from(requested);
    }

    @PATCH
    @Path("/{id}")
    public UserProfileResponse updateUser(@Context HttpHeaders headers, @PathParam("id") UUID id, UpdateUserRequest request) {
        AppUser current = currentUser(headers);
        if (current.role != UserRole.ADMIN) {
            throw new ForbiddenException("Only admins can update other users");
        }
        return UserProfileResponse.from(userService.updateUser(id, request));
    }

    private AppUser currentUser(HttpHeaders headers) {
        AuthenticatedUser identity = authProvider.currentUser(headers);
        return userService.getOrCreateFromIdentity(identity);
    }

    private void requireAtLeastTeacher(AppUser user) {
        if (user.role != UserRole.TEACHER && user.role != UserRole.ADMIN) {
            throw new ForbiddenException("Teacher or admin role required");
        }
    }
}
