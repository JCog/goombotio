
package APIs.Leaderboard;

import java.util.List;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Data {

    @SerializedName("weblink")
    @Expose
    private String weblink;
    @SerializedName("game")
    @Expose
    private String game;
    @SerializedName("category")
    @Expose
    private String category;
    @SerializedName("level")
    @Expose
    private Object level;
    @SerializedName("platform")
    @Expose
    private String platform;
    @SerializedName("region")
    @Expose
    private Object region;
    @SerializedName("emulators")
    @Expose
    private Object emulators;
    @SerializedName("video-only")
    @Expose
    private Boolean videoOnly;
    @SerializedName("timing")
    @Expose
    private String timing;
    @SerializedName("values")
    @Expose
    private Values values;
    @SerializedName("runs")
    @Expose
    private List<Run> runs = null;
    @SerializedName("links")
    @Expose
    private List<Link_> links = null;

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

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Object getLevel() {
        return level;
    }

    public void setLevel(Object level) {
        this.level = level;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public Object getRegion() {
        return region;
    }

    public void setRegion(Object region) {
        this.region = region;
    }

    public Object getEmulators() {
        return emulators;
    }

    public void setEmulators(Object emulators) {
        this.emulators = emulators;
    }

    public Boolean getVideoOnly() {
        return videoOnly;
    }

    public void setVideoOnly(Boolean videoOnly) {
        this.videoOnly = videoOnly;
    }

    public String getTiming() {
        return timing;
    }

    public void setTiming(String timing) {
        this.timing = timing;
    }

    public Values getValues() {
        return values;
    }

    public void setValues(Values values) {
        this.values = values;
    }

    public List<Run> getRuns() {
        return runs;
    }

    public void setRuns(List<Run> runs) {
        this.runs = runs;
    }

    public List<Link_> getLinks() {
        return links;
    }

    public void setLinks(List<Link_> links) {
        this.links = links;
    }

}
