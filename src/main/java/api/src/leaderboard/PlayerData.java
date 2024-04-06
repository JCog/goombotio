package api.src.leaderboard;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PlayerData {
    @JsonProperty("id")
    private String id;
    
    @JsonProperty("names")
    private Names names;
    
    public PlayerData() {}
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public Names getNames() {
        return names;
    }
    
    public void setNames(Names names) {
        this.names = names;
    }
}
