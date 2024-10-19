package api.bluesky.postthread;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PostThread {
    @JsonProperty("thread")
    private Thread thread;
    
    public PostThread() {}
    
    public Thread getThread() {
        return thread;
    }
}
