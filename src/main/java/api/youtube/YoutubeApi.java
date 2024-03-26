package api.youtube;

import api.youtube.video.Item;
import api.youtube.video.Video;
import api.youtube.video.VideoInterface;
import jakarta.ws.rs.ClientErrorException;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;

import java.text.NumberFormat;

public class YoutubeApi {
    private static final String BASE_URI = "https://www.googleapis.com/youtube/v3/";
    
    private final VideoInterface proxy;
    
    public YoutubeApi(ResteasyClient client) {
        ResteasyWebTarget target = client.target(BASE_URI);
        proxy = target.proxy(VideoInterface.class);
    }
    
    public String getVideoDetails(String videoId, String apiKey) {
        Video video;
        try {
            video = proxy.getVideoById(videoId, apiKey, "snippet,statistics");
        } catch (ClientErrorException e) {
            System.out.println("Error getting YouTube video details:\n" + e.getMessage());
            return "";
        }
        
        if (video.getItems().isEmpty()) {
            return "Deleted/Private Youtube Video";
        }
        
        Item item = video.getItems().get(0);
        String title = item.getSnippet().getTitle();
        String channelTitle = item.getSnippet().getChannelTitle();
        Integer viewCount = item.getVideoStats().getViewCount();
        Integer likeCount = item.getVideoStats().getLikeCount();
        
        NumberFormat numberFormat = NumberFormat.getInstance();
        numberFormat.setGroupingUsed(true);
        return String.format(
                "YouTube Video: %s • %s • %s view%s | %s",
                channelTitle,
                title,
                numberFormat.format(viewCount),
                viewCount == 1 ? "" : "s",
                likeCount == null ? "(ratings hidden)" : "👍" + numberFormat.format(likeCount)
        );
    }
}
