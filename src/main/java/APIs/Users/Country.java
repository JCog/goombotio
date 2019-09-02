
package APIs.Users;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Country {

    @SerializedName("code")
    @Expose
    private String code;
    @SerializedName("names")
    @Expose
    private Names_ names;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Names_ getNames() {
        return names;
    }

    public void setNames(Names_ names) {
        this.names = names;
    }

}
