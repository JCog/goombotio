package api.racetime.gamedata;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GameData {
    @JsonProperty("current_races")
    private List<Race> currentRaces;
    
    public GameData() {}
    
    public List<Race> getCurrentRaces() {
        return currentRaces;
    }
    
    public void setCurrentRaces(List<Race> currentRaces) {
        this.currentRaces = currentRaces;
    }
}
