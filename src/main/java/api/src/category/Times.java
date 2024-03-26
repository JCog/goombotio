package api.src.category;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Times {
    @JsonProperty("primary_t")
    private BigDecimal primaryTime;
    
    public Times() {}
    
    public BigDecimal getPrimaryTime() {
        return primaryTime;
    }
    
    public void setPrimaryTime(BigDecimal primaryTime) {
        this.primaryTime = primaryTime;
    }
}
