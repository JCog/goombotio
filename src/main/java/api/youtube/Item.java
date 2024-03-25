package api.youtube;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Item {
    @JsonProperty("statistics")
    private VideoStats videoStats;
    
    @JsonProperty("snippet")
    private Snippet snippet;
    
    public Item() {}
    
    public VideoStats getVideoStats() {
        return videoStats;
    }
    
    public void setVideoStats(VideoStats videoStats) {
        this.videoStats = videoStats;
    }
    
    public Snippet getSnippet() {
        return snippet;
    }
    
    public void setSnippet(Snippet snippet) {
        this.snippet = snippet;
    }
}
