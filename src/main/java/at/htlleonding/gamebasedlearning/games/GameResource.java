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

    @Inject PublicGameView publicView;

    @GET
    public List<com.fasterxml.jackson.databind.node.ObjectNode> findAll() {
        return repository.findAll().stream().filter(GameAccess::publicGame).map(publicView::of).toList();
    }

    @GET
    @Path("/grouped")
    public Map<String, Map<String, List<com.fasterxml.jackson.databind.node.ObjectNode>>> findAllGroupedLikeGamesJson() {
        Map<String, List<com.fasterxml.jackson.databind.node.ObjectNode>> games = findAll().stream()
                .collect(Collectors.groupingBy(
                        game -> game.path("spiel_typ").asText(),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        return Map.of("spiele", games);
    }

    @GET
    @Path("/type/{gameType}")
    public List<com.fasterxml.jackson.databind.node.ObjectNode> findByType(@PathParam("gameType") String gameType) {
        return repository.findByType(gameType).stream().filter(GameAccess::publicGame)
                .map(publicView::of).toList();
    }

    @GET
    @Path("/{id}")
    public com.fasterxml.jackson.databind.node.ObjectNode findById(@Context HttpHeaders headers, @PathParam("id") String id) {
        Game game = repository.findById(id).orElseThrow(NotFoundException::new);
        GameAccess.read(game, GameAccess.publicGame(game) ? null : userService.getOrCreateFromIdentity(authProvider.currentUser(headers)));
        return publicView.of(game);
    }

    @POST
    public Response create(@Context HttpHeaders headers, Game game) {
        AppUser creator = requireTeacher(headers);
        validate(game);
        if (game.id == null || game.id.isBlank()) game.id = java.util.UUID.randomUUID().toString();
        game.teacher = creator.displayName;
        Game createdGame = repository.create(game, creator);
        return Response
                .created(URI.create("/api/games/" + createdGame.id))
                .entity(createdGame)
                .build();
    }

    @PUT
    @Path("/{id}")
    public Game update(@Context HttpHeaders headers, @PathParam("id") String id, Game game) {
        AppUser user = requireTeacher(headers);
        GameAccess.write(repository.findById(id).orElseThrow(NotFoundException::new), user);
        if ("Smart10".equals(game.gameType) || "JustOne".equals(game.gameType)) validate(game);
        return repository.update(id, game)
                .orElseThrow(NotFoundException::new);
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@Context HttpHeaders headers, @PathParam("id") String id) {
        AppUser user = requireTeacher(headers);
        GameAccess.write(repository.findById(id).orElseThrow(NotFoundException::new), user);
        if (!repository.delete(id)) {
            throw new NotFoundException();
        }
        return Response.noContent().build();
    }

    @GET
    @Path("/mine")
    public List<Game> mine(@Context HttpHeaders headers) {
        AppUser user = requireTeacher(headers);
        return repository.findAll().stream().filter(game -> GameAccess.owns(game, user)).toList();
    }

    public record StatusRequest(String status) { }
    @GET
    @Path("/{id}/editor")
    public com.fasterxml.jackson.databind.node.ObjectNode editor(@Context HttpHeaders headers, @PathParam("id") String id) {
        AppUser user = requireTeacher(headers);
        Game game = repository.findById(id).orElseThrow(NotFoundException::new);
        GameAccess.write(game, user);
        return fullContent(game);
    }

    @GET
    @Path("/{id}/library")
    public com.fasterxml.jackson.databind.node.ObjectNode library(@Context HttpHeaders headers, @PathParam("id") String id) {
        AppUser user = requireTeacher(headers);
        Game game = repository.findById(id).orElseThrow(NotFoundException::new);
        GameAccess.read(game, user);
        com.fasterxml.jackson.databind.node.ObjectNode content = fullContent(game);
        com.fasterxml.jackson.databind.node.ObjectNode result = mapper.createObjectNode();
        result.set("spiel_typ", content.path("spiel_typ"));
        result.set("fragen", content.path("fragen"));
        return result;
    }

    private com.fasterxml.jackson.databind.node.ObjectNode fullContent(Game game) {
        com.fasterxml.jackson.databind.node.ObjectNode json = mapper.valueToTree(game);
        for (int i = 0; i < game.questions.size(); i++) {
            ((com.fasterxml.jackson.databind.node.ObjectNode) json.path("fragen").get(i))
                    .set("karten", mapper.valueToTree(game.questions.get(i).smart10Cards));
        }
        return json;
    }
    @Inject com.fasterxml.jackson.databind.ObjectMapper mapper;
    @jakarta.ws.rs.PATCH
    @Path("/{id}/status")
    @jakarta.transaction.Transactional
    public Game status(@Context HttpHeaders headers, @PathParam("id") String id, StatusRequest request) {
        AppUser user = requireTeacher(headers);
        Game game = repository.findById(id).orElseThrow(NotFoundException::new);
        GameAccess.write(game, user);
        if (request == null || request.status() == null || !List.of("PUBLIC", "PRIVATE", "DRAFT").contains(request.status()))
            throw new jakarta.ws.rs.BadRequestException("Ungültiger Status");
        jakarta.persistence.EntityManager em = Game.getEntityManager();
        em.find(Game.class, id).visibility = request.status();
        game.visibility = request.status();
        return game;
    }

    @POST
    @Path("/{id}/copy")
    public Game copy(@Context HttpHeaders headers, @PathParam("id") String id) {
        AppUser user = requireTeacher(headers);
        Game source = repository.findById(id).orElseThrow(NotFoundException::new);
        GameAccess.read(source, user);
        return repository.copyGame(id, java.util.UUID.randomUUID().toString(), user);
    }

    private void validate(Game game) {
        if (game == null || game.name == null || game.name.isBlank() || game.name.length() > 160
                || game.questions == null || game.questions.isEmpty()
                || game.visibility == null || !List.of("PUBLIC", "PRIVATE", "DRAFT").contains(game.visibility)
                || !List.of("Smart10", "JustOne").contains(game.gameType))
            throw new jakarta.ws.rs.BadRequestException("Titel, Spieltyp und Fragen sind erforderlich.");
        for (Question question : game.questions) {
            if (question.text == null || question.text.isBlank()) throw new jakarta.ws.rs.BadRequestException("Fragetext fehlt");
            if ("Smart10".equals(game.gameType)) Smart10Cards.validate(question);
            else if (question.answerOptions == null || question.answerOptions.size() != 5
                    || question.answerOptions.stream().anyMatch(option -> option.text == null || option.text.isBlank()))
                throw new jakarta.ws.rs.BadRequestException("Just One benötigt fünf Begriffe pro Karte.");
        }
    }

    private AppUser requireTeacher(HttpHeaders headers) {
        AuthenticatedUser identity = authProvider.currentUser(headers);
        AppUser user = userService.getOrCreateFromIdentity(identity);
        if (!user.active) {
            throw new ForbiddenException("Active account required");
        }
        if (user.role != UserRole.TEACHER && user.role != UserRole.ADMIN) {
            throw new ForbiddenException("Teacher or admin role required");
        }
        return user;
    }
}
