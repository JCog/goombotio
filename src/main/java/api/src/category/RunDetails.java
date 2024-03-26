package api.src.category;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RunDetails {
    @JsonProperty("players")
    private List<Player> players;
    
    @JsonProperty("times")
    private Times times;
    
    @JsonProperty("values")
    private Map<String, String> values;
    
    public RunDetails() {}
    
    public List<Player> getPlayers() {
        return players;
    }
    
    public void setPlayers(List<Player> players) {
        this.players = players;
    }
    
    public Times getTimes() {
        return times;
    }
    
    public void setTimes(Times times) {
        this.times = times;
    }
    
    public Map<String, String> getValues() {
        return values;
    }
    
    public void setValues(Map<String, String> values) {
        this.values = values;
    }
}
