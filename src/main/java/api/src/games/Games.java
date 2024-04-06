package api.src.games;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Games {
    @JsonProperty("data")
    private List<GameData> gameData;
    
    public Games() {}
    
    public List<GameData> getGameData() {
        return gameData;
    }
    
    public void setGameData(List<GameData> gameData) {
        this.gameData = gameData;
    }
}
