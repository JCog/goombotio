
package APIs.Users;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class ColorFrom {

    @SerializedName("light")
    @Expose
    private String light;
    @SerializedName("dark")
    @Expose
    private String dark;

    public String getLight() {
        return light;
    }

    public void setLight(String light) {
        this.light = light;
    }

    public String getDark() {
        return dark;
    }

    public void setDark(String dark) {
        this.dark = dark;
    }

}
