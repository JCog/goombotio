package dev.jcog.goombotio.util;

import java.util.Map;

public class Settings {
    private static final Map<String, String> ENV = System.getenv();

    public static final Boolean DEV_ENV = ENV.containsKey("DEV_ENV") &&
            ENV.get("DEV_ENV").equalsIgnoreCase("true"); // default false
    public static final Boolean SILENT_MODE = ENV.containsKey("SILENT_MODE") &&
            ENV.get("SILENT_MODE").equalsIgnoreCase("true"); // default false
    public static final Boolean WRITE_PERMISSION = !ENV.containsKey("WRITE_PERMISSION") ||
            ENV.get("WRITE_PERMISSION").equalsIgnoreCase("true"); // default true

    public static final String DB_HOST = ENV.get("DB_HOST");
    public static final Integer DB_PORT = getEnvInt("DB_PORT");
    public static final String DB_USER = ENV.get("DB_USER");
    public static final String DB_PW = ENV.get("DB_PW");

    public static final String TWITCH_CLIENT_ID = ENV.get("TWITCH_CLIENT_ID");
    public static final String TWITCH_CLIENT_SECRET = ENV.get("TWITCH_CLIENT_SECRET");
    public static final String TWITCH_STREAM = ENV.get("TWITCH_STREAM");
    public static final String TWITCH_USER = ENV.get("TWITCH_USER");

    public static final String DISCORD_TOKEN = ENV.get("DISCORD_TOKEN");

    public static final String YT_API_KEY = ENV.get("YT_API_KEY");

    public static final String TWITTER_CONSUMER_KEY = ENV.get("TWITTER_CONSUMER_KEY");
    public static final String TWITTER_CONSUMER_SECRET = ENV.get("TWITTER_CONSUMER_SECRET");
    public static final String TWITTER_ACCESS_TOKEN = ENV.get("TWITTER_ACCESS_TOKEN");
    public static final String TWITTER_ACCESS_TOKEN_SECRET = ENV.get("TWITTER_ACCESS_TOKEN_SECRET");

    private static Integer getEnvInt(String key) {
        try {
            return Integer.valueOf(ENV.get(key));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
