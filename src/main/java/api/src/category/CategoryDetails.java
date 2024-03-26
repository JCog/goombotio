package api.src.category;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CategoryDetails {
    @JsonProperty("runs")
    private List<Run> runs;
    
    public CategoryDetails() {}
    
    public List<Run> getRuns() {
        return runs;
    }
    
    public void setRuns(List<Run> runs) {
        this.runs = runs;
    }
}
