package api.youtube;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

public interface VideoInterface {
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("videos")
    public Video getVideoById(
            @QueryParam("id") String videoId,
            @QueryParam("key") String apiKey,
            @QueryParam("part") String part
    );
}
