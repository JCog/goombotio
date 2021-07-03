package api;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.util.concurrent.TimeUnit;

public class SpeedrunApi extends BaseAPI {

    private static final String BASE_URL = "http://www.speedrun.com/api/v1/";
    private static final String LEADERBOARDS = "leaderboards/";
    private static final String USERS = "users/";

    private static final String DATA_KEY = "data";
    private static final String RUNS_KEY = "runs";
    private static final String RUN_KEY = "run";
    private static final String TIMES_KEY = "times";
    private static final String PRIMARY_T_KEY = "primary_t";
    private static final String PLAYERS_KEY = "players";
    private static final String ID_KEY = "id";
    private static final String NAMES_KEY = "names";
    private static final String INTERNATIONAL_KEY = "international";

    private static final String ERROR_MESSAGE = "The SRC certificate has expired. Tell @JCog to fix it. :)";

    interface Category {
        String getUri();
    }

    public enum Game {
        BUG_FABLES("Bug Fables", "bug_fables/"),
        SUNSHINE("Super Mario Sunshine", "sms/"),
        PAPER_MARIO("Paper Mario", "pm64/"),
        PAPER_MARIO_MEMES("Paper Mario", "pm64memes/"),
        TTYD("Paper Mario: The Thousand-Year Door", "ttyd/");

        private final String name;
        private final String uri;

        Game(String name, String uri) {
            this.name = name;
            this.uri = uri;
        }

        public String getUri() {
            return uri;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public enum BugFablesCategory implements Category {
        ANY_PERCENT("Any%", "any"),
        HUNDO("100%", "100"),
        GLITCHLESS("Any% Glitchless", "any_glitchless"),
        ALL_BOSSES("All Bosses", "all_bosses"),
        ALL_CHAPTERS("All Chapters", "all_chapters"),
        ANY_MYSTERY("Any% MYSTERY?", "any_mystery"),
        ANY_ALL_CODES("Any% All Codes", "any_all_codes"),
        ANY_DLL("Any% dll", "any_dll");

        private final String name;
        private final String uri;

        BugFablesCategory(String name, String uri) {
            this.name = name;
            this.uri = uri;
        }

        public String getUri() {
            return uri;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public enum SunshineCategory implements Category {
        ANY_PERCENT("Any%", "any"),
        ALL_EPISODES("All Episodes", "all_episodes"),
        SHINES_79("79 Shines", "79_shines"),
        SHINES_96("96 Shines", "96_shines"),
        SHINES_120("120 Shines", "120_shines");

        private final String name;
        private final String uri;

        SunshineCategory(String name, String uri) {
            this.name = name;
            this.uri = uri;
        }

        public String getUri() {
            return uri;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public enum PapeCategory implements Category {
        ANY_PERCENT("Any%", "any"),
        ANY_PERCENT_NO_PW("Any% (No PW)", "any_no_pw"),
        ALL_CARDS("All Cards", "all_cards"),
        ALL_BOSSES("All Bosses", "all_bosses"),
        GLITCHLESS("Glitchless", "glitchless"),
        REVERSE_ALL_CARDS("Reverse All Cards", "reverse_all_cards"),
        HUNDO("100%", "100"),
        PIGGIES("5 Golden Lil' Oinks", "5_golden_lil_oinks"),
        ALL_BLOOPS("All Bloops", "all_bloops"),
        ANY_NO_RNG("Any% No RNG", "any_no_rng"),
        BEAT_CHAPTER_1("Beat Chapter 1", "beat_chapter_1"),
        SOAP_CAKE("Soap Cake", "soapcake"),
        MAILMAN("Mailman%", "mailman"),
        NMSB("Any% No Major Sequence Breaks", "any_no_major_sequence_breaks"),
        STOP_N_SWOP("Stop 'n' Swop", "stop_n_swop");

        private final String name;
        private final String uri;

        PapeCategory(String name, String uri) {
            this.name = name;
            this.uri = uri;
        }

        public String getUri() {
            return uri;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public enum TtydCategory implements Category {
        ANY_PERCENT("Any%", "any"),
        ALL_CRYSTAL_STARS("All Crystal Stars", "all_crystal_stars"),
        GLITCHLESS("Glitchless", "glitchless"),
        HUNDO("100%", "100"),
        ALL_COLLECTIBLES("All Collectibles", "all_collectibles"),
        MAX_UPGRADES("Max Upgrades", "max_upgrades");

        private final String name;
        private final String uri;

        TtydCategory(String name, String uri) {
            this.name = name;
            this.uri = uri;
        }

        public String getUri() {
            return uri;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public enum Platform {
        N64("N64", "n64"),
        WII("Wii VC", "wiivc"),
        WIIU("Wii U VC", "wiiuvc");

        private final String name;
        private final String uri;

        Platform(String name, String uri) {
            this.name = name;
            this.uri = uri;
        }

        public String getUri() {
            return uri;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    /**
     * Retrieves the world records for the category specified. Game and category must match.
     *
     * @param game     main or extension leaderboard
     * @param category speedrunning category
     * @return formatted string containing WR(s)
     */
    public static String getWr(Game game, Category category) {
        String gameString = game.getUri();
        String categoryString = category.getUri();

        String json = getWrJson(gameString, categoryString);
        if (json == null) {
            return ERROR_MESSAGE;
        }
        String playerId = getPlayerIdFromJson(json);
        long seconds = getRunTimeFromJson(json);

        String name = getUsernameFromId(playerId);
        String time = getTimeString(seconds);
        return String.format("The %s %s WR is %s by %s", game.toString(), category.toString(), time, name);
    }

    /**
     * Retrieves the platform and overall world records for the category specified. Game, category, and platform must
     * match.
     *
     * @param game     main or extension leaderboard
     * @param category speedrunning category
     * @param platform console to check in addition to overall WR
     * @return formatted string containing WR(s)
     */
    public static String getWr(Game game, Category category, Platform platform) {
        String gameString = game.getUri();
        String categoryString = category.getUri();
        String platformString = platform.getUri();

        String allJson = getWrJson(gameString, categoryString);
        if (allJson == null) {
            return ERROR_MESSAGE;
        }
        String allPlayerId = getPlayerIdFromJson(allJson);
        long allSeconds = getRunTimeFromJson(allJson);

        String allName = getUsernameFromId(allPlayerId);
        String allTime = getTimeString(allSeconds);

        String n64Json = getWrJson(gameString, categoryString, platformString);
        String n64PlayerId = getPlayerIdFromJson(n64Json);

        long n64Seconds = getRunTimeFromJson(n64Json);
        String platformTime = getTimeString(n64Seconds);

        String platformName = getUsernameFromId(n64PlayerId);


        return String.format(
                "The %s %s WRs are %s by %s overall and %s by %s on %s.",
                game.toString(),
                category.toString(),
                allTime,
                allName,
                platformTime,
                platformName,
                platform.toString()
        );
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private static String getPlayerIdFromJson(String json) {
        JSONParser jsonParser = new JSONParser();

        JSONObject leaderboard;
        try {
            leaderboard = (JSONObject) jsonParser.parse(json);
        }
        catch (ParseException e) {
            e.printStackTrace();
            return "";
        }
        JSONObject data = (JSONObject) leaderboard.get(DATA_KEY);
        JSONObject runs_first = (JSONObject) ((JSONArray) data.get(RUNS_KEY)).get(0);
        JSONObject run = (JSONObject) runs_first.get(RUN_KEY);
        JSONObject players_first = (JSONObject) ((JSONArray) run.get(PLAYERS_KEY)).get(0);
        return players_first.get(ID_KEY).toString();
    }

    private static long getRunTimeFromJson(String json) {
        JSONParser jsonParser = new JSONParser();

        JSONObject leaderboard;
        try {
            leaderboard = (JSONObject) jsonParser.parse(json);
        }
        catch (ParseException e) {
            e.printStackTrace();
            return 0;
        }
        JSONObject data = (JSONObject) leaderboard.get(DATA_KEY);
        JSONObject runs_first = (JSONObject) ((JSONArray) data.get(RUNS_KEY)).get(0);
        JSONObject run = (JSONObject) runs_first.get(RUN_KEY);
        JSONObject times = (JSONObject) run.get(TIMES_KEY);
        return (long) times.get(PRIMARY_T_KEY);
    }

    private static String getUsernameFromId(String id) {
        JSONParser jsonParser = new JSONParser();
        String userJson = submitRequest(buildUserUrl(id));

        JSONObject user;
        try {
            user = (JSONObject) jsonParser.parse(userJson);
        }
        catch (ParseException e) {
            e.printStackTrace();
            return "";
        }
        JSONObject data = (JSONObject) user.get(DATA_KEY);
        JSONObject names = (JSONObject) data.get(NAMES_KEY);
        return names.get(INTERNATIONAL_KEY).toString();
    }

    private static String getTimeString(long seconds) {
        long hours = TimeUnit.SECONDS.toHours(seconds);
        seconds -= TimeUnit.HOURS.toSeconds(hours);
        long minutes = TimeUnit.SECONDS.toMinutes(seconds);
        seconds -= TimeUnit.MINUTES.toSeconds(minutes);
        if (hours > 0) {
            return String.format("%d:%02d:%02d", hours, minutes, seconds);
        }
        else {
            return String.format("%d:%02d", minutes, seconds);
        }
    }

    private static String getWrJson(String game, String category, String platform) {
        String url = buildWrUrl(game, category, platform);
        return submitRequest(url);
    }

    private static String getWrJson(String game, String category) {
        String url = buildWrUrl(game, category);
        return submitRequest(url);
    }

    private static String buildUserUrl(String id) {
        return BASE_URL + USERS + id;
    }

    private static String buildWrUrl(String game, String category, String platform) {
        return BASE_URL + LEADERBOARDS + game + "category/" + category + "?top=1&platform=" + platform;
    }

    private static String buildWrUrl(String game, String category) {
        return BASE_URL + LEADERBOARDS + game + "category/" + category + "?top=1";
    }
}