package api.seventv;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("users/twitch/")
public interface UserInterface {
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{userId}")
    public User getUserById(@PathParam("userId") String userId);
}
