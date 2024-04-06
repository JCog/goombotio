package api.src.games;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Categories {
    @JsonProperty("data")
    private List<CategoryData> categoryData;
    
    public Categories() {}
    
    public List<CategoryData> getCategoryData() {
        return categoryData;
    }
    
    public void setCategoryData(List<CategoryData> categoryData) {
        this.categoryData = categoryData;
    }
}
