package Util;

import org.ini4j.Wini;

import java.io.File;
import java.io.IOException;

public class Settings {
    private static final String INI_FILENAME = "settings.ini";
    
    /////////////////////  TAGS  /////////////////////
    private static final String GENERAL_CAT_TAG = "general";
    private static final String GENERAL_VERBOSE_LOGGING_TAG = "verboseLogging";
    private static final String GENERAL_SILENT_MODE_TAG = "silentMode";
    private static final String GENERAL_WRITE_PERMISSION_TAG = "writePermission";
    
    private static final String TWITCH_CAT_TAG = "twitch";
    private static final String TWITCH_STREAM_TAG = "stream";
    private static final String TWITCH_USERNAME_TAG = "username";
    private static final String TWITCH_CHANNEL_AUTH_TOKEN_TAG = "channelAuthToken";
    private static final String TWITCH_CHANNEL_CLIENT_ID_TAG = "channelClientId";
    private static final String TWITCH_BOT_AUTH_TOKEN_TAG = "botAuthToken";
    
    private static final String DISCORD_CAT_TAG = "discord";
    private static final String DISCORD_TOKEN_TAG = "token";
    
    private static final String YOUTUBE_CAT_TAG = "youtube";
    private static final String YOUTUBE_API_KEY_TAG = "apiKey";
    
    private static final String TWITTER_CAT_TAG = "twitter";
    private static final String TWITTER_CONSUMER_KEY_TAG = "consumerKey";
    private static final String TWITTER_CONSUMER_SECRET_TAG = "consumerSecret";
    private static final String TWITTER_ACCESS_TOKEN_TAG = "accessToken";
    private static final String TWITTER_ACCESS_TOKEN_SECRET_TAG = "accessTokenSecret";
    
    /////////////////////  VARS  /////////////////////
    private static boolean VERBOSE_LOGGING;
    private static boolean SILENT_MODE;
    private static boolean WRITE_PERMISSION;
    
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
        
        VERBOSE_LOGGING = ini.get(GENERAL_CAT_TAG, GENERAL_VERBOSE_LOGGING_TAG, boolean.class);
        SILENT_MODE = ini.get(GENERAL_CAT_TAG, GENERAL_SILENT_MODE_TAG, boolean.class);
        WRITE_PERMISSION = ini.get(GENERAL_CAT_TAG, GENERAL_WRITE_PERMISSION_TAG, boolean.class);
    
        TWITCH_STREAM = ini.get(TWITCH_CAT_TAG, TWITCH_STREAM_TAG);
        TWITCH_USERNAME = ini.get(TWITCH_CAT_TAG, TWITCH_USERNAME_TAG);
        TWITCH_CHANNEL_AUTH_TOKEN = ini.get(TWITCH_CAT_TAG, TWITCH_CHANNEL_AUTH_TOKEN_TAG);
        TWITCH_CHANNEL_CLIENT_ID = ini.get(TWITCH_CAT_TAG, TWITCH_CHANNEL_CLIENT_ID_TAG);
        TWITCH_BOT_AUTH_TOKEN = ini.get(TWITCH_CAT_TAG, TWITCH_BOT_AUTH_TOKEN_TAG);
        
        DISCORD_TOKEN = ini.get(DISCORD_CAT_TAG, DISCORD_TOKEN_TAG);
        
        YOUTUBE_API_KEY = ini.get(YOUTUBE_CAT_TAG, YOUTUBE_API_KEY_TAG);
        
        TWITTER_CONSUMER_KEY = ini.get(TWITTER_CAT_TAG, TWITTER_CONSUMER_KEY_TAG);
        TWITTER_CONSUMER_SECRET = ini.get(TWITTER_CAT_TAG, TWITTER_CONSUMER_SECRET_TAG);
        TWITTER_ACCESS_TOKEN = ini.get(TWITTER_CAT_TAG, TWITTER_ACCESS_TOKEN_TAG);
        TWITTER_ACCESS_TOKEN_SECRET = ini.get(TWITTER_CAT_TAG, TWITTER_ACCESS_TOKEN_SECRET_TAG);
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
    
    public static String getTwitchStream() {
        return TWITCH_STREAM;
    }
    
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
}
