package api.src.leaderboard;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Leaderboard {
    @JsonProperty("data")
    private LeaderboardData leaderboardData;
    
    public Leaderboard() {}
    
    public LeaderboardData getLeaderboardData() {
        return leaderboardData;
    }
    
    public void setLeaderboardData(LeaderboardData leaderboardData) {
        this.leaderboardData = leaderboardData;
    }
}
