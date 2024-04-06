package api.src.leaderboard;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Names {
    @JsonProperty("international")
    private String international;
    
    public Names() {}
    
    public String getInternational() {
        return international;
    }
    
    public void setInternational(String international) {
        this.international = international;
    }
}
