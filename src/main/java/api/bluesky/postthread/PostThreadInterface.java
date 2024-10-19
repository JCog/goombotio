package api.bluesky.postthread;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

public interface PostThreadInterface {
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("app.bsky.feed.getPostThread")
    PostThread getPostThread(@QueryParam("uri") String uri);
}
