
package APIs.Leaderboard;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Times {

    @SerializedName("primary")
    @Expose
    private String primary;
    @SerializedName("primary_t")
    @Expose
    private Integer primaryT;
    @SerializedName("realtime")
    @Expose
    private String realtime;
    @SerializedName("realtime_t")
    @Expose
    private Integer realtimeT;
    @SerializedName("realtime_noloads")
    @Expose
    private Object realtimeNoloads;
    @SerializedName("realtime_noloads_t")
    @Expose
    private Integer realtimeNoloadsT;
    @SerializedName("ingame")
    @Expose
    private Object ingame;
    @SerializedName("ingame_t")
    @Expose
    private Integer ingameT;

    public String getPrimary() {
        return primary;
    }

    public void setPrimary(String primary) {
        this.primary = primary;
    }

    public Integer getPrimaryT() {
        return primaryT;
    }

    public void setPrimaryT(Integer primaryT) {
        this.primaryT = primaryT;
    }

    public String getRealtime() {
        return realtime;
    }

    public void setRealtime(String realtime) {
        this.realtime = realtime;
    }

    public Integer getRealtimeT() {
        return realtimeT;
    }

    public void setRealtimeT(Integer realtimeT) {
        this.realtimeT = realtimeT;
    }

    public Object getRealtimeNoloads() {
        return realtimeNoloads;
    }

    public void setRealtimeNoloads(Object realtimeNoloads) {
        this.realtimeNoloads = realtimeNoloads;
    }

    public Integer getRealtimeNoloadsT() {
        return realtimeNoloadsT;
    }

    public void setRealtimeNoloadsT(Integer realtimeNoloadsT) {
        this.realtimeNoloadsT = realtimeNoloadsT;
    }

    public Object getIngame() {
        return ingame;
    }

    public void setIngame(Object ingame) {
        this.ingame = ingame;
    }

    public Integer getIngameT() {
        return ingameT;
    }

    public void setIngameT(Integer ingameT) {
        this.ingameT = ingameT;
    }

}
