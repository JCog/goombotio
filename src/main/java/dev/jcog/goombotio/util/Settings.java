package dev.jcog.goombotio.util;

public class Settings {
    public static final Boolean SILENT_MODE = System.getenv("SILENT_MODE").equalsIgnoreCase("true");
    public static final Boolean WRITE_PERMISSION = System.getenv("WRITE_PERMISSION").equalsIgnoreCase("true");
    public static final String DB_HOST = System.getenv("DB_HOST");
    public static final Integer DB_PORT = Integer.valueOf(System.getenv("DB_PORT"));
    public static final String DB_USER = System.getenv("DB_USER");
    public static final String DB_PW = System.getenv("DB_PW");
    public static final String TWITCH_CLIENT_ID = System.getenv("TWITCH_CLIENT_ID");
    public static final String TWITCH_CLIENT_SECRET = System.getenv("TWITCH_CLIENT_SECRET");
    public static final String TWITCH_STREAM = System.getenv("TWITCH_STREAM");
    public static final String TWITCH_USER = System.getenv("TWITCH_USER");
    public static final String DISCORD_TOKEN = System.getenv("DISCORD_TOKEN");
    public static final String YT_API_KEY = System.getenv("YT_API_KEY");
    public static final String TWITTER_CONSUMER_KEY = System.getenv("TWITTER_CONSUMER_KEY");
    public static final String TWITTER_CONSUMER_SECRET = System.getenv("TWITTER_CONSUMER_SECRET");
    public static final String TWITTER_ACCESS_TOKEN = System.getenv("TWITTER_ACCESS_TOKEN");
    public static final String TWITTER_ACCESS_TOKEN_SECRET = System.getenv("TWITTER_ACCESS_TOKEN_SECRET");
}
