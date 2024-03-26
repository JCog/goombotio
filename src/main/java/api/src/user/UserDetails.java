package api.src.user;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class UserDetails {
    @JsonProperty("names")
    private Names names;
    
    public UserDetails() {}
    
    public Names getNames() {
        return names;
    }
    
    public void setNames(Names names) {
        this.names = names;
    }
}
