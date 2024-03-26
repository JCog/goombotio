package api.ffz.room;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Room {
    @JsonProperty("sets")
    private Map<String, Set> sets;
    
    public Room() {}
    
    public Map<String, Set> getSets() {
        return sets;
    }
    
    public void setSets(Map<String, Set> sets) {
        this.sets = sets;
    }
}
