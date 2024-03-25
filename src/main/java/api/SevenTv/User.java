package api.SevenTv;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class User {
    @JsonProperty("emote_set")
    private EmoteSet emoteSet;
    
    public User() {}
    
    public EmoteSet getEmoteSet() {
        return emoteSet;
    }
    
    public void setEmoteSet(EmoteSet emotes) {
        this.emoteSet = emotes;
    }
}
