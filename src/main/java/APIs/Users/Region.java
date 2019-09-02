
package APIs.Users;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Region {

    @SerializedName("code")
    @Expose
    private String code;
    @SerializedName("names")
    @Expose
    private Names__ names;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Names__ getNames() {
        return names;
    }

    public void setNames(Names__ names) {
        this.names = names;
    }

}
