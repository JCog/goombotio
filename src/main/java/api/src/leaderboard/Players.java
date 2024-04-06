package api.src.leaderboard;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Players {
    @JsonProperty("data")
    private List<PlayerData> playerData;
    
    public Players() {}
    
    public List<PlayerData> getPlayerData() {
        return playerData;
    }
    
    public void setPlayerData(List<PlayerData> playerData) {
        this.playerData = playerData;
    }
}
