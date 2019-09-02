
package APIs.Leaderboard;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Run {

    @SerializedName("place")
    @Expose
    private Integer place;
    @SerializedName("run")
    @Expose
    private Run_ run;

    public Integer getPlace() {
        return place;
    }

    public void setPlace(Integer place) {
        this.place = place;
    }

    public Run_ getRun() {
        return run;
    }

    public void setRun(Run_ run) {
        this.run = run;
    }

}
