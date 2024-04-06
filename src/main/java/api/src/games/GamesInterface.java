package api.src.games;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

public interface GamesInterface {
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("games")
    Games getGameByName(@QueryParam("name") String name, @QueryParam("embed") String embed);
}
