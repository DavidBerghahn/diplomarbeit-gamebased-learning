package at.htlleonding.gamebasedlearning.games;

import at.htlleonding.gamebasedlearning.auth.AuthProvider;
import at.htlleonding.gamebasedlearning.auth.AuthenticatedUser;
import at.htlleonding.gamebasedlearning.users.AppUser;
import at.htlleonding.gamebasedlearning.users.UserRole;
import at.htlleonding.gamebasedlearning.users.UserService;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Path("/api/games")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class GameResource {
    @Inject
    GameRepository repository;

    @Inject
    AuthProvider authProvider;

    @Inject
    UserService userService;

    @GET
    public List<Game> findAll() {
        return repository.findAll();
    }

    @GET
    @Path("/grouped")
    public Map<String, Map<String, List<Game>>> findAllGroupedLikeGamesJson() {
        Map<String, List<Game>> games = repository.findAll().stream()
                .collect(Collectors.groupingBy(
                        game -> game.gameType,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        return Map.of("spiele", games);
    }

    @GET
    @Path("/type/{gameType}")
    public List<Game> findByType(@PathParam("gameType") String gameType) {
        return repository.findByType(gameType);
    }

    @GET
    @Path("/{id}")
    public Game findById(@PathParam("id") String id) {
        return repository.findById(id)
                .orElseThrow(NotFoundException::new);
    }

    @POST
    public Response create(@Context HttpHeaders headers, Game game) {
        AppUser creator = requireTeacher(headers);
        Game createdGame = repository.create(game, creator);
        return Response
                .created(URI.create("/api/games/" + createdGame.id))
                .entity(createdGame)
                .build();
    }

    @PUT
    @Path("/{id}")
    public Game update(@Context HttpHeaders headers, @PathParam("id") String id, Game game) {
        requireTeacher(headers);
        return repository.update(id, game)
                .orElseThrow(NotFoundException::new);
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@Context HttpHeaders headers, @PathParam("id") String id) {
        requireTeacher(headers);
        if (!repository.delete(id)) {
            throw new NotFoundException();
        }
        return Response.noContent().build();
    }

    private AppUser requireTeacher(HttpHeaders headers) {
        AuthenticatedUser identity = authProvider.currentUser(headers);
        AppUser user = userService.getOrCreateFromIdentity(identity);
        if (user.role != UserRole.TEACHER && user.role != UserRole.ADMIN) {
            throw new ForbiddenException("Teacher or admin role required");
        }
        return user;
    }
}
