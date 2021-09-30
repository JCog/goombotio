package database.entries;

public class TattleItem {
    private final String twitchId;
    private final String tattle;

    public TattleItem(String twitchId, String tattle) {
        this.twitchId = twitchId;
        this.tattle = tattle;
    }

    public String getTwitchId() {
        return twitchId;
    }

    public String getTattle() {
        return tattle;
    }
}
