package api.bluesky.postthread;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Post {
    
    @JsonProperty("record")
    private Record record;
    
    @JsonProperty("repostCount")
    private Integer repostCount;
    
    @JsonProperty("likeCount")
    private Integer likeCount;
    
    public Post() {}
    
    public Record getRecord() {
        return record;
    }
    
    public Integer getRepostCount() {
        return repostCount;
    }
    
    public Integer getLikeCount() {
        return likeCount;
    }
}
