package at.htlleonding.gamebasedlearning.system;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.time.Instant;
import java.util.Map;

@Path("/api/health")
@Produces(MediaType.APPLICATION_JSON)
public class HealthResource {
    @GET
    public Map<String, Object> health() {
        return Map.of(
                "status", "UP",
                "service", "gamebased-learning",
                "time", Instant.now().toString()
        );
    }
}
