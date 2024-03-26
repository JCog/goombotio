package api.seventv.user;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class EmoteSet {
    @JsonProperty("emotes")
    private List<Emote> emotes;
    
    public EmoteSet() {}
    
    public List<Emote> getEmotes() {
        return emotes;
    }
    
    public void setEmotes(List<Emote> emotes) {
        this.emotes = emotes;
    }
}
