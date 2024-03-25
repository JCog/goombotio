package api.Ffz;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.MapSerializer;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Room {
    @JsonSerialize(keyUsing = MapSerializer.class)
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
