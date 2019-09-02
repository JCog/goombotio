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
    private static final String GAME_PAPER_MARIO_MEMES = "pmariomemes/";

    private static final String CAT_PAPE_ANY_PERCENT = "any";
    private static final String CAT_PAPE_ANY_NO_PW = "any_no_pw";
    private static final String CAT_PAPE_ALL_CARDS = "all_cards";
    private static final String CAT_PAPE_ALL_BOSSES = "all_bosses";
    private static final String CAT_PAPE_GLITCHLESS = "glitchless";
    private static final String CAT_PAPE_100 = "100";

    private static final String CAT_PAPE_MEMES_PIGGIES = "5_golden_lil_oinks";
    private static final String CAT_PAPE_MEMES_ALL_BLOOPS = "all_bloops";
    private static final String CAT_PAPE_MEMES_ANY_NO_RNG = "any_no_rng";
    private static final String CAT_PAPE_MEMES_BEAT_CHAPTER_1 = "beat_chapter_1";
    private static final String CAT_PAPE_MEMES_SOAP_CAKE = "soapcake";

    private static final String PLATFORM_PAPE_PLATFORM_N64 = "n64";
    private static final String PLATFORM_PAPE_PLATFORM_WII = "wiivc";
    private static final String PLATFORM_PAPE_PLATFORM_WII_U = "wiiuvc";

    public enum Game {
        PAPER_MARIO,
        PAPER_MARIO_MEMES
    }

    public enum PapeCategory {
        ANY_PERCENT,
        ANY_PERCENT_NO_PW,
        ALL_CARDS,
        ALL_BOSSES,
        GLITCHLESS,
        HUNDO,
        PIGGIES,
        ALL_BLOOPS,
        ANY_NO_RNG,
        BEAT_CHAPTER_1,
        SOAP_CAKE
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


        Leaderboard allLeaderboard = gson.fromJson(getWrJson(gameString, categoryString), Leaderboard.class);
        String allPlayerId = allLeaderboard.getData().getRuns().get(0).getRun().getPlayers().get(0).getId();

        long allSeconds = allLeaderboard.getData().getRuns().get(0).getRun().getTimes().getPrimaryT();
        String allTime = getTimeString(allSeconds);

        String allName = getUsernameFromId(allPlayerId);


        if (game.equals(Game.PAPER_MARIO)) {
            String n64 = getPapePlatformUrlString(PapePlatform.N64);

            Leaderboard n64Leaderboard = gson.fromJson(getWrJson(gameString, categoryString, n64), Leaderboard.class);
            String n64PlayerId = n64Leaderboard.getData().getRuns().get(0).getRun().getPlayers().get(0).getId();

            long n64Seconds = n64Leaderboard.getData().getRuns().get(0).getRun().getTimes().getPrimaryT();
            String n64Time = getTimeString(n64Seconds);

            String n64Name = getUsernameFromId(n64PlayerId);


            return String.format("The Paper Mario %s WRs are %s by %s overall and %s by %s on N64.", getPapeCategoryString(category), allTime, allName, n64Time, n64Name);
        }
        else {
            return String.format("The Paper Mario %s WR is %s by %s", getPapeCategoryString(category), allTime, allName);
        }
    }

    private static String getUsernameFromId(String id) {
        Gson gson = new Gson();
        String userJson = submitRequest(buildUserUrl(id));

        User user = gson.fromJson(userJson, User.class);
        return user.getData().getNames().getInternational();
    }

    private static String getTimeString(long seconds) {
        long hours = TimeUnit.SECONDS.toHours(seconds);
        seconds -= TimeUnit.HOURS.toSeconds(hours);
        long minutes = TimeUnit.SECONDS.toMinutes(seconds);
        seconds -= TimeUnit.MINUTES.toSeconds(minutes);
        if (hours > 0) {
            return String.format("%d:%d:%d", hours, minutes, seconds);
        }
        else {
            return String.format("%d:%d", minutes, seconds);
        }
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
            case PIGGIES:
                return "5 Golden Lil' Oinks";
            case ALL_BLOOPS:
                return "All Bloops";
            case ANY_NO_RNG:
                return "Any% No RNG";
            case BEAT_CHAPTER_1:
                return "Beat Chapter 1";
            case SOAP_CAKE:
                return "Soap Cake";
            default:
                return "Any%";
        }
    }

    private static String getGameUrlString(Game game) {
        switch (game) {
            case PAPER_MARIO:
                return GAME_PAPER_MARIO;
            case PAPER_MARIO_MEMES:
                return GAME_PAPER_MARIO_MEMES;
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
            case PIGGIES:
                return CAT_PAPE_MEMES_PIGGIES;
            case ALL_BLOOPS:
                return CAT_PAPE_MEMES_ALL_BLOOPS;
            case ANY_NO_RNG:
                return CAT_PAPE_MEMES_ANY_NO_RNG;
            case BEAT_CHAPTER_1:
                return CAT_PAPE_MEMES_BEAT_CHAPTER_1;
            case SOAP_CAKE:
                return CAT_PAPE_MEMES_SOAP_CAKE;
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