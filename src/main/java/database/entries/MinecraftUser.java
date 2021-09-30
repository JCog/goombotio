package database.entries;

public class MinecraftUser {
    private final String twitchId;
    private final String mcUuid;
    private final String mcUsername;

    public MinecraftUser(String twitchId, String mcUuid, String mcUsername) {
        this.twitchId = twitchId;
        this.mcUuid = mcUuid;
        this.mcUsername = mcUsername;
    }

    public String getTwitchId() {
        return twitchId;
    }

    public String getMcUuid() {
        return mcUuid;
    }

    public String getMcUsername() {
        return mcUsername;
    }
}
