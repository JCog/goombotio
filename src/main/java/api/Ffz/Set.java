package api.Ffz;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Set {
    @JsonProperty("emoticons")
    private List<Emoticon> emoticons;
    
    public Set() {}
    
    public List<Emoticon> getEmoticons() {
        return emoticons;
    }
    
    public void setEmoticons(List<Emoticon> emoticons) {
        this.emoticons = emoticons;
    }
}
