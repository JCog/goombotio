package api.src.leaderboard;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class LeaderboardData {
    @JsonProperty("runs")
    private List<Run> runList;
    
    @JsonProperty("players")
    private Players players;
    
    public LeaderboardData() {}
    
    public List<Run> getRunList() {
        return runList;
    }
    
    public void setRunList(List<Run> runList) {
        this.runList = runList;
    }
    
    public Players getPlayers() {
        return players;
    }
    
    public void setPlayers(Players players) {
        this.players = players;
    }
}
