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
    @jakarta.inject.Inject
    jakarta.persistence.EntityManager database;
    @GET
    public jakarta.ws.rs.core.Response health() {
        boolean connected;
        try { connected = ((Number) database.createNativeQuery("select 1").getSingleResult()).intValue() == 1; }
        catch (RuntimeException failure) { connected = false; }
        return jakarta.ws.rs.core.Response.status(connected ? 200 : 503).entity(Map.of(
                "status", connected ? "UP" : "DOWN",
                "database", connected ? "UP" : "DOWN",
                "service", "gamebased-learning",
                "time", Instant.now().toString()
        )).build();
    }
}
