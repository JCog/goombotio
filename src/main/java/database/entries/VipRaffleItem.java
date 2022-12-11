package database.entries;

public class VipRaffleItem {
    private final String twitchId;
    private final int entryCount;

    public VipRaffleItem(String twitchId, int entryCount) {
        this.twitchId = twitchId;
        this.entryCount = entryCount;
    }

    public String getTwitchId() {
        return twitchId;
    }

    public int getEntryCount() {
        return entryCount;
    }
}
