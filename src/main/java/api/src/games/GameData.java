package api.src.games;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GameData {
    @JsonProperty("id")
    private String id;
    
    @JsonProperty("names")
    private Names names;
    
    @JsonProperty("categories")
    private Categories categories;
    
    public GameData() {}
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public Categories getCategories() {
        return categories;
    }
    
    public Names getNames() {
        return names;
    }
    
    public void setNames(Names names) {
        this.names = names;
    }
    
    public void setCategories(Categories categories) {
        this.categories = categories;
    }
}
