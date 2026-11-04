package at.htlleonding.gamebasedlearning.games;

import at.htlleonding.gamebasedlearning.auth.AuthProvider;
import at.htlleonding.gamebasedlearning.users.AppUser;
import at.htlleonding.gamebasedlearning.users.UserService;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.inject.Inject;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;

import java.util.UUID;

@Path("/api/smart10/solo")
public class Smart10SoloResource {
    @Inject AuthProvider auth;
    @Inject UserService users;
    @Inject Smart10SoloService solo;
    @Inject GameRepository games;

    public record StartRequest(String gameId) { }
    public record AnswerRequest(UUID cardId, JsonNode answer) { }

    @POST
    @Path("/start")
    public Smart10SoloService.Start start(@Context HttpHeaders headers, StartRequest request) {
        if (request == null || request.gameId() == null) throw new IllegalArgumentException("Game ID required");
        AppUser viewer = user(headers);
        GameAccess.read(games.findById(request.gameId()).orElseThrow(jakarta.ws.rs.NotFoundException::new), viewer);
        return solo.start(viewer.id, request.gameId());
    }

    @POST
    @Path("/{roundId}/answers")
    public Smart10SoloService.Feedback answer(@Context HttpHeaders headers,
            @PathParam("roundId") UUID roundId, AnswerRequest request) {
        if (request == null || request.cardId() == null) throw new IllegalArgumentException("Card ID required");
        return solo.answer(user(headers).id, roundId, request.cardId(), request.answer());
    }

    @POST
    @Path("/{roundId}/finish")
    public Smart10SoloService.Finish finish(@Context HttpHeaders headers, @PathParam("roundId") UUID roundId) {
        return solo.finish(user(headers).id, roundId);
    }

    @DELETE
    @Path("/{roundId}")
    public void cancel(@Context HttpHeaders headers, @PathParam("roundId") UUID roundId) {
        solo.cancel(user(headers).id, roundId);
    }

    private AppUser user(HttpHeaders headers) {
        AppUser user = users.getOrCreateFromIdentity(auth.currentUser(headers));
        if (!user.active) throw new IllegalArgumentException("Active account required");
        return user;
    }
}
