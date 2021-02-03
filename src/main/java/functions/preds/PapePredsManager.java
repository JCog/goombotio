package functions.preds;

import com.gikk.twirk.types.users.TwitchUser;
import com.jcog.utils.database.DbManager;
import com.jcog.utils.database.preds.PredsLeaderboardDb;
import util.TwirkInterface;

import java.util.*;

import static java.lang.System.out;

public class PapePredsManager extends PredsManagerBase {

    private static final String DISCORD_CHANNEL_MONTHLY = "pape-preds-monthly";
    private static final String DISCORD_CHANNEL_ALL_TIME = "pape-preds-all-time";
    private static final int POINTS_3 = 20;
    private static final int POINTS_2 = 5;
    private static final int POINTS_1 = 1;
    private static final int POINTS_WRONG_ORDER = 1;
    private static final Set<String> BADGE_CHOICES = new HashSet<>(Arrays.asList(
            "badspin1",
            "badspin2",
            "badspin3",
            "spoodlyspun"
    ));

    public enum Badge {
        BAD_SPIN1,
        BAD_SPIN2,
        BAD_SPIN3,
        SPOODLY_SPUN
    }

    private final HashMap<Long,PapePredsObject> predictionList = new HashMap<>();

    /**
     * Manages the !preds Twitch chat game.
     *
     * @param twirk twirk for chat
     */
    public PapePredsManager(TwirkInterface twirk, DbManager dbManager) {
        super(twirk, dbManager);
    }

    /**
     * Given the three correct badges, sets the game to an ended state, determines and records points won, and sends a
     * message to the chat to let them know who, if anyone, got all three correct, as well as the current monthly
     * leaderboard.
     */
    @Override
    public void submitPredictions(String answer) {
        Badge one = Badge.values()[Character.getNumericValue(answer.charAt(0)) - 1];
        Badge two = Badge.values()[Character.getNumericValue(answer.charAt(1)) - 1];
        Badge three = Badge.values()[Character.getNumericValue(answer.charAt(2)) - 1];
        enabled = false;
        waitingForAnswer = false;

        ArrayList<String> winners = getWinners(one, two, three);
        StringBuilder message = new StringBuilder();
        if (winners.size() == 0) {
            message.append("Nobody guessed it. jcogThump Hopefully you got some points, though!");
        }
        else if (winners.size() == 1) {
            message.append(String.format("Congrats to @%s on guessing correctly! jcogChamp", winners.get(0)));
        }
        else if (winners.size() == 2) {
            message.append(String.format("Congrats to @%s and @%s on guessing correctly! jcogChamp",
                                         winners.get(0),
                                         winners.get(1)));
        }
        else {
            message.append("Congrats to ");
            for (int i = 0; i < winners.size() - 1; i++) {
                message.append("@").append(winners.get(i)).append(", ");
            }
            message.append("and @").append(winners.get(winners.size() - 1));
            message.append(" on guessing correctly! jcogChamp");
        }
        message.append(" • ");
        message.append(buildMonthlyLeaderboardString());

        twirk.channelCommand(String.format("/me The correct answer was %s %s %s - %s",
                                           badgeToString(one),
                                           badgeToString(two),
                                           badgeToString(three),
                                           message.toString()));
        updateDiscordMonthlyPoints();
        updateDiscordAllTimePoints();
    }

    @Override
    public String getAnswerRegex() {
        return "[1-4]{3}";
    }

    @Override
    public void makePredictionIfValid(TwitchUser user, String message) {
        String[] split = message.split("\\s");

        if (split.length == 3) {
            //using FFZ emotes
            ArrayList<String> badgeGuess = new ArrayList<>();
            for (String word : split) {
                if (BADGE_CHOICES.contains(word.toLowerCase()) && !badgeGuess.contains(word.toLowerCase())) {
                    badgeGuess.add(word.toLowerCase());
                }
            }

            if (badgeGuess.size() == 3) {
                predictionList.put(user.getUserID(), new PapePredsObject(
                        user,
                        stringToBadge(badgeGuess.get(0)),
                        stringToBadge(badgeGuess.get(1)),
                        stringToBadge(badgeGuess.get(2))
                ));
                System.out.printf("%s has predicted %s %s %s%n",
                                  user.getUserName(), badgeGuess.get(0), badgeGuess.get(1), badgeGuess.get(2));
            }
        }
        else if (split.length == 1 && split[0].matches("[1-4]{3}")) {
            //using numbers, e.g. "412"
            Vector<Integer> badgeGuess = new Vector<>();
            for (int i = 0; i < split[0].length(); i++) {
                int guess = Character.getNumericValue(split[0].charAt(i));
                if (!badgeGuess.contains(guess)) {
                    badgeGuess.add(guess);
                }
            }

            if (badgeGuess.size() == 3) {
                Badge badge1 = intToBadge(badgeGuess.get(0));
                Badge badge2 = intToBadge(badgeGuess.get(1));
                Badge badge3 = intToBadge(badgeGuess.get(2));

                predictionList.put(user.getUserID(), new PapePredsObject(user, badge1, badge2, badge3));

                System.out.printf("%s has predicted %s %s %s%n", user.getUserName(),
                                  badgeToString(badge1), badgeToString(badge2), badgeToString(badge3));
            }
        }
    }

    private ArrayList<String> getWinners(Badge leftAnswer, Badge middleAnswer, Badge rightAnswer) {
        Set<Badge> answerSet = new HashSet<>();
        answerSet.add(leftAnswer);
        answerSet.add(middleAnswer);
        answerSet.add(rightAnswer);

        ArrayList<String> winners = new ArrayList<>();
        for (PapePredsObject pred : predictionList.values()) {
            TwitchUser user = pred.getTwitchUser();
            Badge leftGuess = pred.getLeft();
            Badge middleGuess = pred.getMiddle();
            Badge rightGuess = pred.getRight();
            Set<Badge> guessSet = new HashSet<>();
            guessSet.add(leftGuess);
            guessSet.add(middleGuess);
            guessSet.add(rightGuess);

            if (leftGuess == leftAnswer && middleGuess == middleAnswer && rightGuess == rightAnswer) {
                winners.add(user.getDisplayName());
                leaderboard.addPointsAndWins(user, POINTS_3, 1);
                out.printf("%s guessed 3 correctly. Adding %d points and a win.%n", user.getDisplayName(),
                           POINTS_3);
            }
            else if ((leftGuess == leftAnswer && middleGuess == middleAnswer) ||
                    (leftGuess == leftAnswer && rightGuess == rightAnswer) ||
                    (middleGuess == middleAnswer && rightGuess == rightAnswer)) {
                leaderboard.addPoints(user, POINTS_2);
                out.printf("%s guessed 2 correctly. Adding %d points.%n", user.getDisplayName(), POINTS_2);
            }
            else if (leftGuess == leftAnswer || middleGuess == middleAnswer || rightGuess == rightAnswer) {
                leaderboard.addPoints(user, POINTS_1);
                out.printf("%s guessed 1 correctly. Adding %d point.%n", user.getDisplayName(), POINTS_1);
            }
            else if (answerSet.equals(guessSet)) {
                leaderboard.addPoints(user, POINTS_WRONG_ORDER);
                out.printf("%s guessed 0 correctly, but got all 3 badges. Adding %d point.%n",
                           user.getDisplayName(), POINTS_WRONG_ORDER);
            }
            else {
                out.printf("%s guessed 0 correctly.%n", user.getDisplayName());
            }
        }
        return winners;
    }

    @Override
    protected PredsLeaderboardDb getLeaderboardType() {
        return dbManager.getSpeedySpinLeaderboardDb();
    }

    @Override
    protected String getMonthlyChannelName() {
        return DISCORD_CHANNEL_MONTHLY;
    }

    @Override
    protected String getAllTimeChannelName() {
        return DISCORD_CHANNEL_ALL_TIME;
    }

    @Override
    protected String getStartMessage() {
        return "/me Get your predictions in! Send a message with three of either BadSpin1 BadSpin2 " +
                "BadSpin3 or SpoodlySpun (or a message with 3 digits from 1 to 4) to guess the order the badges will " +
                "show up in the badge shop! If you get all three right and don't have a sub, you'll win one! Type " +
                "!preds to learn more.";
    }

    /**
     * Converts a {@link Badge} to a {@link String}
     *
     * @param badge badge to convert to String
     * @return String for Badge
     */
    public static String badgeToString(Badge badge) {
        switch (badge) {
            case BAD_SPIN1:
                return "BadSpin1";
            case BAD_SPIN2:
                return "BadSpin2";
            case BAD_SPIN3:
                return "BadSpin3";
            default:
                return "SpoodlySpun";
        }
    }

    /**
     * Converts a {@link String} to a {@link Badge}. Returns null if no match exists
     *
     * @param badge badge in String form to convert
     * @return Badge or null
     */
    public static Badge stringToBadge(String badge) {
        switch (badge.toLowerCase()) {
            case "badspin1":
                return Badge.BAD_SPIN1;
            case "badspin2":
                return Badge.BAD_SPIN2;
            case "badspin3":
                return Badge.BAD_SPIN3;
            case "spoodlyspun":
                return Badge.SPOODLY_SPUN;
            default:
                return null;
        }
    }

    /**
     * Converts an int to a {@link Badge}. Returns null if no match exists
     *
     * @param badge badge in int form to convert
     * @return Badge or null
     */
    public static Badge intToBadge(int badge) {
        switch (badge) {
            case 1:
                return Badge.BAD_SPIN1;
            case 2:
                return Badge.BAD_SPIN2;
            case 3:
                return Badge.BAD_SPIN3;
            case 4:
                return Badge.SPOODLY_SPUN;
            default:
                return null;
        }
    }
}
