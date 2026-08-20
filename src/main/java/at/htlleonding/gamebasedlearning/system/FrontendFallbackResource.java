package at.htlleonding.gamebasedlearning.system;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.io.InputStream;

@Path("/")
public class FrontendFallbackResource {
    @GET
    @Path("{path:.*}")
    @Produces(MediaType.TEXT_HTML)
    public Response frontend(@PathParam("path") String path) {
        if (path.startsWith("api") || path.startsWith("q") || path.startsWith("user-socket")) {
            throw new NotFoundException();
        }

        InputStream index = Thread.currentThread()
                .getContextClassLoader()
                .getResourceAsStream("META-INF/resources/index.html");
        if (index == null) {
            throw new NotFoundException();
        }
        return Response.ok(index).build();
    }
}
