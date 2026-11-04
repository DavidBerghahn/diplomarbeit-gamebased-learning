package at.htlleonding.gamebasedlearning.games;

import jakarta.ws.rs.core.*;
import jakarta.ws.rs.ext.*;
import java.util.Map;

@Provider
public class GameValidationMapper implements ExceptionMapper<IllegalArgumentException> {
    @Override public Response toResponse(IllegalArgumentException failure) {
        return Response.status(400).type(MediaType.APPLICATION_JSON_TYPE)
                .entity(Map.of("message",failure.getMessage()==null?"Ungültige Eingabe":failure.getMessage())).build();
    }
}
