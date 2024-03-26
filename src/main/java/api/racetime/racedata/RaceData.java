package api.racetime.racedata;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RaceData {
    @JsonProperty("entrants")
    private List<Entrant> entrants;
    
    @JsonProperty("url")
    private String url;
    
    public RaceData() {}
    
    public List<Entrant> getEntrants() {
        return entrants;
    }
    
    public void setEntrants(List<Entrant> entrants) {
        this.entrants = entrants;
    }
    
    public String getUrl() {
        return url;
    }
    
    public void setUrl(String url) {
        this.url = url;
    }
}
