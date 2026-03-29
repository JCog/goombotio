package dev.jcog.goombotio.api.youtube;

import dev.jcog.goombotio.api.youtube.video.Item;
import dev.jcog.goombotio.api.youtube.video.Video;
import dev.jcog.goombotio.api.youtube.video.VideoInterface;
import jakarta.ws.rs.ClientErrorException;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.NumberFormat;

public class YoutubeApi {
    private static final Logger log = LoggerFactory.getLogger(YoutubeApi.class);
    private static final String BASE_URI = "https://www.googleapis.com/youtube/v3/";

    private final VideoInterface proxy;
    
    public YoutubeApi(ResteasyClient client) {
        ResteasyWebTarget target = client.target(BASE_URI);
        proxy = target.proxy(VideoInterface.class);
    }
    
    public String getVideoDetails(String videoId, String apiKey, boolean isShort) {
        Video video;
        try {
            video = proxy.getVideoById(videoId, apiKey, "snippet,statistics");
        } catch (ClientErrorException e) {
            log.error("Error getting YouTube video details: {}", e.getMessage());
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
                "YouTube %s: %s • %s • %s view%s | %s",
                isShort ? "Short" : "Video",
                channelTitle,
                title,
                numberFormat.format(viewCount),
                viewCount == 1 ? "" : "s",
                likeCount == null ? "(ratings hidden)" : "👍" + numberFormat.format(likeCount)
        );
    }
}
