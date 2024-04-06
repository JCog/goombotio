package api.src.leaderboard;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Run {
    @JsonProperty("run")
    private RunData runData;
    
    public Run() {}
    
    public RunData getRunData() {
        return runData;
    }
    
    public void setRunData(RunData runData) {
        this.runData = runData;
    }
}
