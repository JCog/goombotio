package api.src.category;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Run {
    @JsonProperty("run")
    private RunDetails runDetails;
    
    public Run() {}
    
    public RunDetails getRunDetails() {
        return runDetails;
    }
    
    public void setRunDetails(RunDetails runDetails) {
        this.runDetails = runDetails;
    }
}
