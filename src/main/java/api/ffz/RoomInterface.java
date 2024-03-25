package api.ffz;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("room/")
public interface RoomInterface {
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{username}")
    public Room getRoomById(@PathParam("username") String username);
}
