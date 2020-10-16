package util;

import org.ini4j.Wini;

import java.io.File;
import java.io.IOException;

public class Settings {
    private static final String INI_FILENAME = "settings.ini";

    /////////////////////  TAGS  /////////////////////
    private static final String GENERAL_CAT_TAG = "general";
    private static final String DB_CAT_TAG = "database";
    private static final String TWITCH_CAT_TAG = "twitch";
    private static final String DISCORD_CAT_TAG = "discord";
    private static final String YOUTUBE_CAT_TAG = "youtube";
    private static final String TWITTER_CAT_TAG = "twitter";
    private static final String MINECRAFT_CAT_TAG = "minecraft";

    /////////////////////  VARS  /////////////////////
    private static boolean VERBOSE_LOGGING;
    private static boolean SILENT_MODE;
    private static boolean WRITE_PERMISSION;

    private static String DB_HOST;
    private static int DB_PORT;
    private static String DB_USER;
    private static String DB_PASSWORD;

    private static String TWITCH_STREAM;
    private static String TWITCH_USERNAME;
    private static String TWITCH_CHANNEL_AUTH_TOKEN;
    private static String TWITCH_CHANNEL_CLIENT_ID;
    private static String TWITCH_BOT_AUTH_TOKEN;

    private static String DISCORD_TOKEN;

    private static String YOUTUBE_API_KEY;

    private static String TWITTER_CONSUMER_KEY;
    private static String TWITTER_CONSUMER_SECRET;
    private static String TWITTER_ACCESS_TOKEN;
    private static String TWITTER_ACCESS_TOKEN_SECRET;

    private static String MINECRAFT_SERVER;
    private static String MINECRAFT_USER;
    private static String MINECRAFT_PASSWORD;
    private static String MINECRAFT_WHITELIST_LOCATION;

    //////////////////////////////////////////////////
    public static void init() {
        //noinspection MismatchedQueryAndUpdateOfCollection
        Wini ini;
        try {
            ini = new Wini(new File(INI_FILENAME));
        }
        catch (IOException e) {
            System.out.println("IOException reading ini, exiting");
            System.exit(1);
            return;
        }

        VERBOSE_LOGGING = ini.get(GENERAL_CAT_TAG, "verboseLogging", boolean.class);
        SILENT_MODE = ini.get(GENERAL_CAT_TAG, "silentMode", boolean.class);
        WRITE_PERMISSION = ini.get(GENERAL_CAT_TAG, "writePermission", boolean.class);

        DB_HOST = ini.get(DB_CAT_TAG, "host");
        DB_PORT = ini.get(DB_CAT_TAG, "port", int.class);
        DB_USER = ini.get(DB_CAT_TAG, "user");
        DB_PASSWORD = ini.get(DB_CAT_TAG, "password");

        TWITCH_STREAM = ini.get(TWITCH_CAT_TAG, "stream");
        TWITCH_USERNAME = ini.get(TWITCH_CAT_TAG, "username");
        TWITCH_CHANNEL_AUTH_TOKEN = ini.get(TWITCH_CAT_TAG, "channelAuthToken");
        TWITCH_CHANNEL_CLIENT_ID = ini.get(TWITCH_CAT_TAG, "channelClientId");
        TWITCH_BOT_AUTH_TOKEN = ini.get(TWITCH_CAT_TAG, "botAuthToken");

        DISCORD_TOKEN = ini.get(DISCORD_CAT_TAG, "token");

        YOUTUBE_API_KEY = ini.get(YOUTUBE_CAT_TAG, "apiKey");

        TWITTER_CONSUMER_KEY = ini.get(TWITTER_CAT_TAG, "consumerKey");
        TWITTER_CONSUMER_SECRET = ini.get(TWITTER_CAT_TAG, "consumerSecret");
        TWITTER_ACCESS_TOKEN = ini.get(TWITTER_CAT_TAG, "accessToken");
        TWITTER_ACCESS_TOKEN_SECRET = ini.get(TWITTER_CAT_TAG, "accessTokenSecret");

        MINECRAFT_SERVER = ini.get(MINECRAFT_CAT_TAG, "server");
        MINECRAFT_USER = ini.get(MINECRAFT_CAT_TAG, "user");
        MINECRAFT_PASSWORD = ini.get(MINECRAFT_CAT_TAG, "password");
        MINECRAFT_WHITELIST_LOCATION = ini.get(MINECRAFT_CAT_TAG, "whitelistLocation");
    }

    public static boolean isVerboseLogging() {
        return VERBOSE_LOGGING;
    }

    public static boolean isSilentMode() {
        return SILENT_MODE;
    }

    public static boolean hasWritePermission() {
        return WRITE_PERMISSION;
    }

    public static String getDbHost() {
        return DB_HOST;
    }

    public static int getDbPort() {
        return DB_PORT;
    }

    public static String getDbUser() {
        return DB_USER;
    }

    public static String getDbPassword() {
        return DB_PASSWORD;
    }

    //streamer username in all lowercase
    public static String getTwitchStream() {
        return TWITCH_STREAM;
    }

    //bot username in all lowercase
    public static String getTwitchUsername() {
        return TWITCH_USERNAME;
    }

    public static String getTwitchChannelAuthToken() {
        return TWITCH_CHANNEL_AUTH_TOKEN;
    }

    public static String getTwitchChannelClientId() {
        return TWITCH_CHANNEL_CLIENT_ID;
    }

    public static String getTwitchChannel() {
        return '#' + TWITCH_STREAM;
    }

    public static String getTwitchChannelOauth() {
        return "oauth:" + TWITCH_CHANNEL_AUTH_TOKEN;
    }

    public static String getTwitchBotOauth() {
        return "oauth:" + TWITCH_BOT_AUTH_TOKEN;
    }

    public static String getDiscordToken() {
        return DISCORD_TOKEN;
    }

    public static String getYoutubeApiKey() {
        return YOUTUBE_API_KEY;
    }

    public static String getTwitterConsumerKey() {
        return TWITTER_CONSUMER_KEY;
    }

    public static String getTwitterConsumerSecret() {
        return TWITTER_CONSUMER_SECRET;
    }

    public static String getTwitterAccessToken() {
        return TWITTER_ACCESS_TOKEN;
    }

    public static String getTwitterAccessTokenSecret() {
        return TWITTER_ACCESS_TOKEN_SECRET;
    }

    public static String getMinecraftServer() {
        return MINECRAFT_SERVER;
    }

    public static String getMinecraftUser() {
        return MINECRAFT_USER;
    }

    public static String getMinecraftPassword() {
        return MINECRAFT_PASSWORD;
    }

    public static String getMinecraftWhitelistLocation() {
        return MINECRAFT_WHITELIST_LOCATION;
    }
}
