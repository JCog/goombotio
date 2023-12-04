package api;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.math.BigDecimal;
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
    
    interface Variable {
        String getVarId();
        String getValueId();
    }

    public enum Game {
        BUG_FABLES("Bug Fables", "bug_fables/"),
        SUNSHINE("Super Mario Sunshine", "sms/"),
        PAPER_MARIO("Paper Mario", "pm64/"),
        PAPER_MARIO_MEMES("Paper Mario", "pm64memes/"),
        TTYD("Paper Mario: The Thousand-Year Door", "ttyd/"),
        OOT("The Legend of Zelda: Ocarina of Time", "oot/"),
        SMRPG("Super Mario RPG (Switch)", "Super_Mario_RPG_Switch/");

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
    
    public enum SmrpgCategory implements Category {
        NORMAL_RTA("Normal RTA", "beat-the-game");
        
        private final String name;
        private final String uri;
        
        SmrpgCategory(String name, String uri) {
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
    
    public enum OotCategory implements Category {
        ANY_PERCENT("Any%", "any");
        
        private final String name;
        private final String uri;
        
        OotCategory(String name, String uri) {
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
    
    public enum PapeVariable implements Variable {
        N64("N64", "r8r5y2le", "jq6vjo71"),
        WII("Wii VC", "r8r5y2le", "5lm2934q"),
        WII_U("Wii U VC", "r8r5y2le", "81w7k25q"),
        SWITCH("Switch", "r8r5y2le", "jqz2x3kq");
        
        private final String name;
        private final String varId;
        private final String valueId;
        
        PapeVariable(String name, String varId, String valueId) {
            this.name = name;
            this.varId = varId;
            this.valueId = valueId;
        }
        
        @Override
        public String getVarId() {
            return varId;
        }
        
        @Override
        public String getValueId() {
            return valueId;
        }
        
        @Override
        public String toString() {
            return name;
        }
    }
    
    public enum SmrpgVariable implements Variable {
        TURBO("Turbo", "p85pvv7l", "q5vo93ml"),
        NO_TURBO("No Turbo", "p85pvv7l", "le2k6x5l");
        
        private final String name;
        private final String varId;
        private final String valueId;
        
        SmrpgVariable(String name, String varId, String valueId) {
            this.name = name;
            this.varId = varId;
            this.valueId = valueId;
        }
        
        @Override
        public String getVarId() {
            return varId;
        }
        
        @Override
        public String getValueId() {
            return valueId;
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
        long ms = getRunTimeMsFromJson(json);

        String name = getUsernameFromId(playerId);
        String time = getTimeString(ms);
        return String.format("The %s %s WR is %s by %s", game, category, time, name);
    }
    
    public static String getWr(Game game, Category category, Variable variable) {
        String gameString = game.getUri();
        String categoryString = category.getUri();
        String varId = variable.getVarId();
        String valueId = variable.getValueId();
        
        String json = getWrJson(gameString, categoryString, varId, valueId);
        if (json == null) {
            return ERROR_MESSAGE;
        }
        String playerId = getPlayerIdFromJson(json);
        long ms = getRunTimeMsFromJson(json);
        
        String name = getUsernameFromId(playerId);
        String time = getTimeString(ms);
        return String.format("The %s %s (%s) WR is %s by %s", game, category, variable, time, name);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private static String getPlayerIdFromJson(String json) {
        JSONParser jsonParser = new JSONParser();

        JSONObject leaderboard;
        try {
            leaderboard = (JSONObject) jsonParser.parse(json);
        } catch (ParseException e) {
            e.printStackTrace();
            return "";
        }
        JSONObject data = (JSONObject) leaderboard.get(DATA_KEY);
        JSONObject runs_first = (JSONObject) ((JSONArray) data.get(RUNS_KEY)).get(0);
        JSONObject run = (JSONObject) runs_first.get(RUN_KEY);
        JSONObject players_first = (JSONObject) ((JSONArray) run.get(PLAYERS_KEY)).get(0);
        return players_first.get(ID_KEY).toString();
    }

    private static long getRunTimeMsFromJson(String json) {
        JSONParser jsonParser = new JSONParser();
        BigDecimal thousand = new BigDecimal(1000);

        JSONObject leaderboard;
        try {
            leaderboard = (JSONObject) jsonParser.parse(json);
        } catch (ParseException e) {
            e.printStackTrace();
            return 0;
        }
        JSONObject data = (JSONObject) leaderboard.get(DATA_KEY);
        JSONObject runs_first = (JSONObject) ((JSONArray) data.get(RUNS_KEY)).get(0);
        JSONObject run = (JSONObject) runs_first.get(RUN_KEY);
        JSONObject times = (JSONObject) run.get(TIMES_KEY);
        BigDecimal seconds;
        if (times.get(PRIMARY_T_KEY).getClass() == Double.class) {
            seconds = BigDecimal.valueOf((Double) times.get(PRIMARY_T_KEY));
        } else {
            seconds = BigDecimal.valueOf((Long) times.get(PRIMARY_T_KEY));
        }
        
        return seconds.multiply(thousand).longValue();
    }

    private static String getUsernameFromId(String id) {
        JSONParser jsonParser = new JSONParser();
        String userJson = submitRequest(buildUserUrl(id));

        JSONObject user;
        try {
            user = (JSONObject) jsonParser.parse(userJson);
        } catch (ParseException e) {
            e.printStackTrace();
            return "";
        }
        JSONObject data = (JSONObject) user.get(DATA_KEY);
        JSONObject names = (JSONObject) data.get(NAMES_KEY);
        return names.get(INTERNATIONAL_KEY).toString();
    }

    private static String getTimeString(long ms) {
        long hours = TimeUnit.MILLISECONDS.toHours(ms);
        ms -= TimeUnit.HOURS.toMillis(hours);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(ms);
        ms -= TimeUnit.MINUTES.toMillis(minutes);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(ms);
        ms -= TimeUnit.SECONDS.toMillis(seconds);
        if (hours > 0) {
            if (ms > 0) {
                return String.format("%d:%02d:%02d.%03d", hours, minutes, seconds, ms);
            }
            return String.format("%d:%02d:%02d", hours, minutes, seconds);
        } else {
            if (ms > 0) {
                return String.format("%d:%02d.%03d", minutes, seconds, ms);
            }
            return String.format("%d:%02d", minutes, seconds);
        }
    }

    private static String getWrJson(String game, String category) {
        String url = buildWrUrl(game, category);
        return submitRequest(url);
    }
    
    private static String getWrJson(String game, String category, String varId, String valueId) {
        String url = buildWrUrl(game, category, varId, valueId);
        return submitRequest(url);
    }

    private static String buildUserUrl(String id) {
        return BASE_URL + USERS + id;
    }

    private static String buildWrUrl(String game, String category) {
        return BASE_URL + LEADERBOARDS + game + "category/" + category + "?top=1";
    }
    
    private static String buildWrUrl(String game, String category, String varId, String valueId) {
        return String.format(
                "%s%s%scategory/%s?top=1&var-%s=%s",
                BASE_URL,
                LEADERBOARDS,
                game,
                category,
                varId,
                valueId
        );
    }
}