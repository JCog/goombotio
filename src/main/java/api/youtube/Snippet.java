package api.youtube;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Snippet {
    @JsonProperty("title")
    private String title;
    
    @JsonProperty("channelTitle")
    private String channelTitle;
    
    public Snippet() {}
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getChannelTitle() {
        return channelTitle;
    }
    
    public void setChannelTitle(String channelTitle) {
        this.channelTitle = channelTitle;
    }
}
