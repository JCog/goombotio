package api.bttv.user;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class User {
    @JsonProperty("channelEmotes")
    private List<Emote> emotes;
    
    public User() {}
    
    public List<Emote> getEmotes() {
        return emotes;
    }
    
    public void setEmotes(List<Emote> emotes) {
        this.emotes = emotes;
    }
}
