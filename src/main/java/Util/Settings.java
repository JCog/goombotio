package Util;

import org.ini4j.Wini;

import java.io.File;
import java.io.IOException;

public class Settings {
    private static boolean VERBOSE;
    
    private static String TWITCH_STREAM;
    private static String TWITCH_USERNAME;
    private static String TWITCH_AUTH_TOKEN;
    
    private static String DISCORD_TOKEN;
    
    private static String YOUTUBE_API_KEY;
    
    private static String TWITTER_CONSUMER_KEY;
    private static String TWITTER_CONSUMER_SECRET;
    private static String TWITTER_ACCESS_TOKEN;
    private static String TWITTER_ACCESS_TOKEN_SECRET;
    
    private static final String INI_FILENAME = "settings.ini";
    
    public static void init() {
        Wini ini;
        try {
            ini = new Wini(new File(INI_FILENAME));
        }
        catch (IOException e) {
            System.out.println("IOException reading ini, exiting");
            System.exit(1);
            return;
        }
        
        VERBOSE = ini.get("general", "verbose", boolean.class);
        
        TWITCH_STREAM = ini.get("twitch", "stream");
        TWITCH_USERNAME = ini.get("twitch", "username");
        TWITCH_AUTH_TOKEN = ini.get("twitch", "authToken");
        
        DISCORD_TOKEN = ini.get("discord", "token");
        
        YOUTUBE_API_KEY = ini.get("youtube", "apiKey");
        
        TWITTER_CONSUMER_KEY = ini.get("twitter", "consumerKey");
        TWITTER_CONSUMER_SECRET = ini.get("twitter", "consumerSecret");
        TWITTER_ACCESS_TOKEN = ini.get("twitter", "accessToken");
        TWITTER_ACCESS_TOKEN_SECRET = ini.get("twitter", "accessTokenSecret");
    }
    
    public static boolean isVerbose() {
        return VERBOSE;
    }
    
    public static String getTwitchStream() {
        return TWITCH_STREAM;
    }
    
    public static String getTwitchUsername() {
        return TWITCH_USERNAME;
    }
    
    public static String getTwitchAuthToken() {
        return TWITCH_AUTH_TOKEN;
    }
    
    public static String getTwitchChannel() {
        return '#' + TWITCH_STREAM;
    }
    
    public static String getTwitchOauth() {
        return "oauth:" + TWITCH_AUTH_TOKEN;
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
