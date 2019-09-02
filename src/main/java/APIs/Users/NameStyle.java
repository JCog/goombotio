
package APIs.Users;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class NameStyle {

    @SerializedName("style")
    @Expose
    private String style;
    @SerializedName("color-from")
    @Expose
    private ColorFrom colorFrom;
    @SerializedName("color-to")
    @Expose
    private ColorTo colorTo;

    public String getStyle() {
        return style;
    }

    public void setStyle(String style) {
        this.style = style;
    }

    public ColorFrom getColorFrom() {
        return colorFrom;
    }

    public void setColorFrom(ColorFrom colorFrom) {
        this.colorFrom = colorFrom;
    }

    public ColorTo getColorTo() {
        return colorTo;
    }

    public void setColorTo(ColorTo colorTo) {
        this.colorTo = colorTo;
    }

}
