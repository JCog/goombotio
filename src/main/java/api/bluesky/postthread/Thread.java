package api.bluesky.postthread;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Thread {
    @JsonProperty("post")
    private Post post;
    
    public Thread() {}
    
    public Post getPost() {
        return post;
    }
}
