package api.src.leaderboard;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

public interface LeaderboardInterface {
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("leaderboards/{game}/category/{category}")
    Leaderboard getWr(
            @PathParam("game") String game,
            @PathParam("category") String category,
            @QueryParam("top") Integer top,
            @QueryParam("embed") String embed,
            @BeanParam VariablesInput variables
    );
}
