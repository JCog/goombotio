package api.src.user;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

public interface UserInterface {
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("users/{userId}")
    User getUserById(@PathParam("userId") String userId);
}
