package api.src.category;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

public interface CategoryInterface {
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("leaderboards/{game}/category/{category}")
    Category getWr(
            @PathParam("game") String game,
            @PathParam("category") String category,
            @QueryParam("top") Integer top
    );
}
