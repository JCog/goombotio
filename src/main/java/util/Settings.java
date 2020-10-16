package util;

import org.ini4j.Wini;

import java.io.File;
import java.io.IOException;

public class Settings {
    private static final String INI_FILENAME = "settings.ini";

    private static final String GENERAL_CAT_TAG = "general";
    private static final String DB_CAT_TAG = "database";
    private static final String TWITCH_CAT_TAG = "twitch";
    private static final String DISCORD_CAT_TAG = "discord";
    private static final String YOUTUBE_CAT_TAG = "youtube";
    private static final String TWITTER_CAT_TAG = "twitter";
    private static final String MINECRAFT_CAT_TAG = "minecraft";

    private final Wini ini = getIni();

    public boolean isVerboseLogging() {
        return ini.get(GENERAL_CAT_TAG, "verboseLogging", boolean.class);
    }

    public boolean isSilentMode() {
        return ini.get(GENERAL_CAT_TAG, "silentMode", boolean.class);
    }

    public boolean hasWritePermission() {
        return ini.get(GENERAL_CAT_TAG, "writePermission", boolean.class);
    }

    public String getDbHost() {
        return ini.get(DB_CAT_TAG, "host");
    }

    public int getDbPort() {
        return ini.get(DB_CAT_TAG, "port", int.class);
    }

    public String getDbUser() {
        return ini.get(DB_CAT_TAG, "user");
    }

    public String getDbPassword() {
        return ini.get(DB_CAT_TAG, "password");
    }

    //streamer username in all lowercase
    public String getTwitchStream() {
        return ini.get(TWITCH_CAT_TAG, "stream");
    }

    //bot username in all lowercase
    public String getTwitchUsername() {
        return ini.get(TWITCH_CAT_TAG, "username");
    }

    public String getTwitchChannelAuthToken() {
        return ini.get(TWITCH_CAT_TAG, "channelAuthToken");
    }

    public String getTwitchChannelClientId() {
        return ini.get(TWITCH_CAT_TAG, "channelClientId");
    }

    public String getTwitchChannel() {
        return '#' + getTwitchStream();
    }

    public String getTwitchChannelOauth() {
        return "oauth:" + getTwitchChannelAuthToken();
    }

    public String getTwitchBotOauth() {
        return "oauth:" + ini.get(TWITCH_CAT_TAG, "botAuthToken");
    }

    public String getDiscordToken() {
        return ini.get(DISCORD_CAT_TAG, "token");
    }

    public String getYoutubeApiKey() {
        return ini.get(YOUTUBE_CAT_TAG, "apiKey");
    }

    public String getTwitterConsumerKey() {
        return ini.get(TWITTER_CAT_TAG, "consumerKey");
    }

    public String getTwitterConsumerSecret() {
        return ini.get(TWITTER_CAT_TAG, "consumerSecret");
    }

    public String getTwitterAccessToken() {
        return ini.get(TWITTER_CAT_TAG, "accessToken");
    }

    public String getTwitterAccessTokenSecret() {
        return ini.get(TWITTER_CAT_TAG, "accessTokenSecret");
    }

    public String getMinecraftServer() {
        return ini.get(MINECRAFT_CAT_TAG, "server");
    }

    public String getMinecraftUser() {
        return ini.get(MINECRAFT_CAT_TAG, "user");
    }

    public String getMinecraftPassword() {
        return ini.get(MINECRAFT_CAT_TAG, "password");
    }

    public String getMinecraftWhitelistLocation() {
        return ini.get(MINECRAFT_CAT_TAG, "whitelistLocation");
    }

    private Wini getIni() {
        //noinspection MismatchedQueryAndUpdateOfCollection
        Wini ini;
        try {
            ini = new Wini(new File(INI_FILENAME));
        }
        catch (IOException e) {
            System.out.println("IOException reading ini");
            return null;
        }
        return ini;
    }
}
