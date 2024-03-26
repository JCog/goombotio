package api.src.category;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Category {
    @JsonProperty("data")
    private CategoryDetails categoryDetails;
    
    public Category() {}
    
    public CategoryDetails getCategoryDetails() {
        return categoryDetails;
    }
    
    public void setCategoryDetails(CategoryDetails categoryDetails) {
        this.categoryDetails = categoryDetails;
    }
}
