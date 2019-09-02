
package APIs.Leaderboard;

import java.util.List;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Run_ {

    @SerializedName("id")
    @Expose
    private String id;
    @SerializedName("weblink")
    @Expose
    private String weblink;
    @SerializedName("game")
    @Expose
    private String game;
    @SerializedName("level")
    @Expose
    private Object level;
    @SerializedName("category")
    @Expose
    private String category;
    @SerializedName("videos")
    @Expose
    private Videos videos;
    @SerializedName("comment")
    @Expose
    private String comment;
    @SerializedName("status")
    @Expose
    private Status status;
    @SerializedName("players")
    @Expose
    private List<Player> players = null;
    @SerializedName("date")
    @Expose
    private String date;
    @SerializedName("submitted")
    @Expose
    private String submitted;
    @SerializedName("times")
    @Expose
    private Times times;
    @SerializedName("system")
    @Expose
    private System system;
    @SerializedName("splits")
    @Expose
    private Object splits;
    @SerializedName("values")
    @Expose
    private Values_ values;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getWeblink() {
        return weblink;
    }

    public void setWeblink(String weblink) {
        this.weblink = weblink;
    }

    public String getGame() {
        return game;
    }

    public void setGame(String game) {
        this.game = game;
    }

    public Object getLevel() {
        return level;
    }

    public void setLevel(Object level) {
        this.level = level;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Videos getVideos() {
        return videos;
    }

    public void setVideos(Videos videos) {
        this.videos = videos;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public List<Player> getPlayers() {
        return players;
    }

    public void setPlayers(List<Player> players) {
        this.players = players;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getSubmitted() {
        return submitted;
    }

    public void setSubmitted(String submitted) {
        this.submitted = submitted;
    }

    public Times getTimes() {
        return times;
    }

    public void setTimes(Times times) {
        this.times = times;
    }

    public System getSystem() {
        return system;
    }

    public void setSystem(System system) {
        this.system = system;
    }

    public Object getSplits() {
        return splits;
    }

    public void setSplits(Object splits) {
        this.splits = splits;
    }

    public Values_ getValues() {
        return values;
    }

    public void setValues(Values_ values) {
        this.values = values;
    }

}
