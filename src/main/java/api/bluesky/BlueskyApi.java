package api.bluesky;

import api.bluesky.postthread.Post;
import api.bluesky.postthread.PostThread;
import api.bluesky.postthread.PostThreadInterface;
import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.InternalServerErrorException;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;

import java.text.NumberFormat;

public class BlueskyApi {
    private static final String BASE_URI = "https://public.api.bsky.app/xrpc/";
    
    private final PostThreadInterface postThreadProxy;
    
    public BlueskyApi(ResteasyClient client) {
        ResteasyWebTarget target = client.target(BASE_URI);
        postThreadProxy = target.proxy(PostThreadInterface.class);
    }
    
    public String getPostDetails(String handle, String postId) {
        String uri = String.format("at://%s/app.bsky.feed.post/%s", handle, postId);
        PostThread thread;
        try {
            thread = postThreadProxy.getPostThread(uri);
        } catch (ClientErrorException|InternalServerErrorException e) {
            System.out.println("Error getting Bluesky post details:\n" + e.getMessage());
            return "";
        }
        
        Post post = thread.getThread().getPost();
        String text = post.getRecord().getText();
        int reposts = post.getRepostCount();
        int likes = post.getLikeCount();
        
        NumberFormat numberFormat = NumberFormat.getInstance();
        numberFormat.setGroupingUsed(true);
        return String.format(
                "Post by @%s: %s • \uD83D\uDD01%s | ❤%s",
                handle,
                text.replaceAll("\\n", " "),
                numberFormat.format(reposts),
                numberFormat.format(likes)
        );
    }
}
