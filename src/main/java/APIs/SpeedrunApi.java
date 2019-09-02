package APIs;

import APIs.Leaderboard.Leaderboard;
import APIs.Users.User;
import com.google.gson.Gson;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.util.concurrent.TimeUnit;

public class SpeedrunApi {

    private static final String BASE_URL = "http://www.speedrun.com/api/v1/";
    private static final String LEADEROARDS = "leaderboards/";
    private static final String USERS = "users/";

    private static final String GAME_PAPER_MARIO = "pmario/";

    private static final String CAT_PAPE_ANY_PERCENT = "any";
    private static final String CAT_PAPE_ANY_NO_PW = "any_no_pw";
    private static final String CAT_PAPE_ALL_CARDS = "all_cards";
    private static final String CAT_PAPE_ALL_BOSSES = "all_bosses";
    private static final String CAT_PAPE_GLITCHLESS = "glitchless";
    private static final String CAT_PAPE_100 = "100";

    private static final String PLATFORM_PAPE_PLATFORM_N64 = "n64";
    private static final String PLATFORM_PAPE_PLATFORM_WII = "wiivc";
    private static final String PLATFORM_PAPE_PLATFORM_WII_U = "wiiuvc";

    public enum Game {
        PAPER_MARIO
    }

    public enum PapeCategory {
        ANY_PERCENT,
        ANY_PERCENT_NO_PW,
        ALL_CARDS,
        ALL_BOSSES,
        GLITCHLESS,
        HUNDO
    }

    public enum PapePlatform {
        N64,
        WII,
        WIIU
    }

    private static OkHttpClient client = new OkHttpClient();

    public static String getPapeWr(Game game, PapeCategory category) {
        Gson gson = new Gson();
        String gameString = getGameUrlString(game);
        String categoryString = getPapeCategoryUrlString(category);
        String n64 = getPapePlatformUrlString(PapePlatform.N64);


        Leaderboard allLeaderboard = gson.fromJson(getWrJson(gameString, categoryString), Leaderboard.class);
        String allPlayerId = allLeaderboard.getData().getRuns().get(0).getRun().getPlayers().get(0).getId();

        long allSeconds = allLeaderboard.getData().getRuns().get(0).getRun().getTimes().getPrimaryT();
        long allHours = TimeUnit.SECONDS.toHours(allSeconds);
        allSeconds -= TimeUnit.HOURS.toSeconds(allHours);
        long allMinutes = TimeUnit.SECONDS.toMinutes(allSeconds);
        allSeconds -= TimeUnit.MINUTES.toSeconds(allMinutes);
        String allTime = String.format("%d:%d:%d", allHours, allMinutes, allSeconds);

        String allName = getUsernameFromId(allPlayerId);


        Leaderboard n64Leaderboard = gson.fromJson(getWrJson(gameString, categoryString, n64), Leaderboard.class);
        String n64PlayerId = n64Leaderboard.getData().getRuns().get(0).getRun().getPlayers().get(0).getId();

        long n64Seconds = n64Leaderboard.getData().getRuns().get(0).getRun().getTimes().getPrimaryT();
        long n64Hours = TimeUnit.SECONDS.toHours(n64Seconds);
        n64Seconds -= TimeUnit.HOURS.toSeconds(n64Hours);
        long n64Minutes = TimeUnit.SECONDS.toMinutes(n64Seconds);
        n64Seconds -= TimeUnit.MINUTES.toSeconds(n64Minutes);
        String n64Time = String.format("%d:%d:%d", n64Hours, n64Minutes, n64Seconds);

        String n64Name = getUsernameFromId(n64PlayerId);


        return String.format("The Paper Mario %s WRs are %s by %s overall and %s by %s on N64.", getPapeCategoryString(category), allTime, allName, n64Time, n64Name);
    }

    private static String getUsernameFromId(String id) {
        Gson gson = new Gson();
        String userJson = submitRequest(buildUserUrl(id));

        User user = gson.fromJson(userJson, User.class);
        return user.getData().getNames().getInternational();
    }

    private static String getPapeCategoryString(PapeCategory category) {
        switch (category) {
            case ANY_PERCENT:
                return "Any%";
            case ANY_PERCENT_NO_PW:
                return "Any% (No PW)";
            case ALL_CARDS:
                return "All Cards";
            case ALL_BOSSES:
                return "All Bosses";
            case GLITCHLESS:
                return "Glitchless";
            case HUNDO:
                return "100%";
            default:
                return "Any%";
        }
    }

    private static String getGameUrlString(Game game) {
        switch (game) {
            default:
                return GAME_PAPER_MARIO;
        }
    }

    private static String getPapeCategoryUrlString(PapeCategory category) {
        switch (category) {
            case ANY_PERCENT:
                return CAT_PAPE_ANY_PERCENT;
            case ANY_PERCENT_NO_PW:
                return CAT_PAPE_ANY_NO_PW;
            case ALL_CARDS:
                return CAT_PAPE_ALL_CARDS;
            case ALL_BOSSES:
                return CAT_PAPE_ALL_BOSSES;
            case GLITCHLESS:
                return CAT_PAPE_GLITCHLESS;
            case HUNDO:
                return CAT_PAPE_100;
            default:
                return CAT_PAPE_ANY_PERCENT;
        }
    }

    private static String getPapePlatformUrlString(PapePlatform platform) {
        switch (platform) {
            case N64:
                return PLATFORM_PAPE_PLATFORM_N64;
            case WII:
                return PLATFORM_PAPE_PLATFORM_WII;
            case WIIU:
                return PLATFORM_PAPE_PLATFORM_WII_U;
            default:
                return PLATFORM_PAPE_PLATFORM_N64;
        }
    }

    private static String getWrJson(String game, String category, String platform) {
        String url = buildWrUrl(game, category,platform);
        return submitRequest(url);
    }
    private static String getWrJson(String game, String category) {
        String url = buildWrUrl(game, category);
        return submitRequest(url);
    }

    private static String submitRequest(String url) {
        Request request = new Request.Builder()
                .url(url)
                .build();
        try (Response response = client.newCall(request).execute()) {
            return response.body().string();
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static String buildUserUrl(String id) {
        return BASE_URL + USERS + id;
    }

    private static String buildWrUrl(String game, String category, String platform) {
        return BASE_URL + LEADEROARDS + game + "category/" + category + "?top=1&platform=" + platform;
    }

    private static String buildWrUrl(String game, String category) {
        return BASE_URL + LEADEROARDS + game + "category/" + category + "?top=1";
    }


}