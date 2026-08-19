package at.htl.boundary;

import at.htl.repositories.Repository;
import jakarta.inject.Inject;
import jakarta.ws.rs.Path;

@Path("/api/console")
public class Ressource {

    @Inject
    Repository repository;



}
