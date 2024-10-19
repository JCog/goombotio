package api.bluesky.postthread;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Record {
    @JsonProperty("text")
    private String text;
    
    public Record() {}
    
    public String getText() {
        return text;
    }
}
