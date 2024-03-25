package api.racetime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Race {
    @JsonProperty("data_url")
    private String data_url;
    
    public Race() {}
    
    public String getData_url() {
        return data_url;
    }
    
    public void setData_url(String data_url) {
        this.data_url = data_url;
    }
}
