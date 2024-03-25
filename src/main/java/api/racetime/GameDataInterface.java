package api.racetime;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

public interface GameDataInterface {
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{gameSlug}/data")
    public GameData getGameData(@PathParam("gameSlug") String gameSlug);
}
