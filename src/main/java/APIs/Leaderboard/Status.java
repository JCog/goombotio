
package APIs.Leaderboard;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Status {

    @SerializedName("status")
    @Expose
    private String status;
    @SerializedName("examiner")
    @Expose
    private String examiner;
    @SerializedName("verify-date")
    @Expose
    private String verifyDate;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getExaminer() {
        return examiner;
    }

    public void setExaminer(String examiner) {
        this.examiner = examiner;
    }

    public String getVerifyDate() {
        return verifyDate;
    }

    public void setVerifyDate(String verifyDate) {
        this.verifyDate = verifyDate;
    }

}
