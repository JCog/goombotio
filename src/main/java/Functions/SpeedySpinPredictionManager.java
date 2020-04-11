package Functions;

import Listeners.Commands.SpeedySpinPredictionListener;
import Util.Database.SpeedySpinLeaderboard;
import com.gikk.twirk.Twirk;
import com.gikk.twirk.types.users.TwitchUser;

import java.text.SimpleDateFormat;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static java.lang.System.out;

public class SpeedySpinPredictionManager {
    
    private static final String DISCORD_CHANNEL_MONTHLY = "preds-monthly";
    private static final String DISCORD_CHANNEL_ALL_TIME = "preds-all-time";
    private static final int DISCORD_MAX_CHARS = 2000;
    private static final int POINTS_3 = 20;
    private static final int POINTS_2 = 5;
    private static final int POINTS_1 = 1;
    private static final int POINTS_WRONG_ORDER = 1;
    
    private final Twirk twirk;
    private final SpeedySpinLeaderboard leaderboard;
    private final DiscordBotController dbc;
    
    private SpeedySpinPredictionListener sspListener;
    
    public enum Badge {
        BAD_SPIN1,
        BAD_SPIN2,
        BAD_SPIN3,
        SPOODLY_SPUN
    }

    private HashMap<TwitchUser, ArrayList<Badge>> predictionList;
    private boolean enabled;
    private boolean waitingForAnswer;
    
    /**
     * Manages the !preds Twitch chat game.
     * @param twirk twirk for chat
     */
    public SpeedySpinPredictionManager(Twirk twirk) {
        this.twirk = twirk;
        enabled = false;
        waitingForAnswer = false;
        predictionList = new HashMap<>();
        leaderboard = SpeedySpinLeaderboard.getInstance();
        dbc = DiscordBotController.getInstance();
    }
    
    /**
     * Submits a prediction for the badge order of the badge shop. All three badges should be unique. If the user has
     * already has a recorded prediction, the old one is removed and replaced with the current one.
     * @param user user making the prediction
     * @param first left badge
     * @param second middle badge
     * @param third right badge
     */
    public void makePrediction(TwitchUser user, Badge first, Badge second, Badge third) {
        if (enabled) {
            ArrayList<Badge> prediction = new ArrayList<>();
            prediction.add(first);
            prediction.add(second);
            prediction.add(third);

            //apparently every user object is unique even if it's the same user, so you can't assume a 2nd prediction
            //from the same user will overwrite the first one. I should probably use the id as a key, but then I don't
            //have easy access to the user object. TODO: fix this
            Iterator<Map.Entry<TwitchUser, ArrayList<Badge>>> it = predictionList.entrySet().iterator();
            while(it.hasNext()) {
                if (it.next().getKey().getUserID() == user.getUserID()) {
                    it.remove();
                    break;
                }
            }
            predictionList.put(user, prediction);
        }
    }
    
    
    /**
     * Starts the listener for the game, sets the game to an enabled state, and sends a message to the chat to tell
     * users to begin submitting predictions.
     */
    public void start() {
        enabled = true;
        twirk.addIrcListener(sspListener = new SpeedySpinPredictionListener(this));
        twirk.channelMessage("/me Get your predictions in! Send a message with three of either BadSpin1 BadSpin2 " +
                "BadSpin3 or SpoodlySpun (or a message with 3 digits from 1 to 4) to guess the order the badges will " +
                "show up in the badge shop! If you get all three right and don't have a sub, you'll win one! Type " +
                "!badgeshop to learn more.");
    }
    
    /**
     * Removes the listener for the game, sets the game to a state where it's waiting for the correct answer, and sends
     * a message to the chat to let them know to stop submitting predictions.
     */
    public void stop() {
        waitingForAnswer = true;
        twirk.removeIrcListener(sspListener);
        twirk.channelMessage("/me Predictions are up! Let's see how everyone did...");
    }
    
    /**
     * Given the three correct badges, sets the game to an ended state, determines and records points won, and sends a
     * message to the chat to let them know who, if anyone, got all three correct, as well as the current monthly
     * leaderboard.
     * @param one left badge
     * @param two middle badge
     * @param three right badge
     */
    public void submitPredictions(Badge one, Badge two, Badge three) {
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
            message.append(String.format("Congrats to @%s and @%s on guessing correctly! jcogChamp", winners.get(0), winners.get(1)));
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

        twirk.channelMessage(String.format("/me The correct answer was %s %s %s - %s",
                badgeToString(one), badgeToString(two), badgeToString(three), message.toString()));
        updateDiscordMonthlyPoints();
        updateDiscordAllTimePoints();
    }
    
    /**
     * Returns true if there is an active !preds game
     * @return enabled state
     */
    public boolean isEnabled() {
        return enabled;
    }
    
    /**
     * Returns true if the game is waiting on the correct answer
     * @return waiting for answer state
     */
    public boolean isWaitingForAnswer() {
        return waitingForAnswer;
    }
    
    private String buildMonthlyLeaderboardString() {
        ArrayList<Long> topMonthlyScorers = leaderboard.getTopMonthlyScorers();
        ArrayList<Integer> topMonthlyPoints = new ArrayList<>();
        ArrayList<String> topMonthlyNames = new ArrayList<>();
        
        for (Long topMonthlyScorer : topMonthlyScorers) {
            topMonthlyPoints.add(leaderboard.getMonthlyPoints(topMonthlyScorer));
            topMonthlyNames.add(leaderboard.getUsername(topMonthlyScorer));
        }
        
        int prevPoints = -1;
        int prevRank = -1;
        ArrayList<String> leaderboardStrings = new ArrayList<>();
        for (int i = 0; i < topMonthlyNames.size(); i++) {
            if (topMonthlyPoints.get(i) != prevPoints) {
                prevRank = i + 1;
                if (prevRank > 5) {
                    break;
                }
            }
            prevPoints = topMonthlyPoints.get(i);
            String name = topMonthlyNames.get(i);
            
            leaderboardStrings.add(String.format("%d. %s - %d", prevRank, name, prevPoints));
        }
        
        return "Monthly Leaderboard: " + String.join(", ", leaderboardStrings);
    }

    private ArrayList<String> getWinners(Badge leftAnswer, Badge middleAnswer, Badge rightAnswer) {
        Set<Badge> answerSet = new HashSet<>();
        answerSet.add(leftAnswer);
        answerSet.add(middleAnswer);
        answerSet.add(rightAnswer);
        
        ArrayList<String> winners = new ArrayList<>();
        for (Map.Entry<TwitchUser, ArrayList<Badge>> pred : predictionList.entrySet()) {
            TwitchUser user = pred.getKey();
            Badge leftGuess = pred.getValue().get(0);
            Badge middleGuess = pred.getValue().get(1);
            Badge rightGuess = pred.getValue().get(2);
            Set<Badge> guessSet = new HashSet<>();
            guessSet.add(leftGuess);
            guessSet.add(middleGuess);
            guessSet.add(rightGuess);

            if (leftGuess == leftAnswer && middleGuess == middleAnswer && rightGuess == rightAnswer) {
                winners.add(pred.getKey().getDisplayName());
                leaderboard.addPointsAndWins(user, POINTS_3, 1);
                out.println(String.format("%s guessed 3 correctly. Adding %d points and a win.", user.getDisplayName(),
                        POINTS_3));
            }
            else if ((leftGuess == leftAnswer && middleGuess == middleAnswer) ||
                    (leftGuess == leftAnswer && rightGuess == rightAnswer) ||
                    (middleGuess == middleAnswer && rightGuess == rightAnswer)) {
                leaderboard.addPoints(user, POINTS_2);
                out.println(String.format("%s guessed 2 correctly. Adding %d points.", user.getDisplayName(), POINTS_2));
            }
            else if (leftGuess == leftAnswer || middleGuess == middleAnswer || rightGuess == rightAnswer) {
                leaderboard.addPoints(user, POINTS_1);
                out.println(String.format("%s guessed 1 correctly. Adding %d point.", user.getDisplayName(), POINTS_1));
            }
            else if (answerSet.equals(guessSet)) {
                leaderboard.addPoints(user, POINTS_WRONG_ORDER);
                out.println(String.format("%s guessed 0 correctly, but got all 3 badges. Adding %d point.",
                        user.getDisplayName(), POINTS_WRONG_ORDER));
            }
            else {
                out.println(String.format("%s guessed 0 correctly.", user.getDisplayName()));
            }
        }
        return winners;
    }
    
    private void updateDiscordMonthlyPoints() {
        ArrayList<Long> topScorers = leaderboard.getTopMonthlyScorers();
        ArrayList<Integer> topPoints = new ArrayList<>();
        ArrayList<String> topNames = new ArrayList<>();
        for (Long topScorer : topScorers) {
            topPoints.add(leaderboard.getMonthlyPoints(topScorer));
            topNames.add(leaderboard.getUsername(topScorer));
        }
        
        StringBuilder message = new StringBuilder();
        SimpleDateFormat sdf = new SimpleDateFormat("MMMM yyyy");
        message.append(sdf.format(new Date()));
        message.append("\n```");
        
        //add entries until discord character limit is reached
        int prevPoints = -1;
        int prevRank = -1;
        for (int i = 0; i < topNames.size(); i++) {
            if (topPoints.get(i) != prevPoints) {
                prevRank = i + 1;
            }
            prevPoints = topPoints.get(i);
            String entry = String.format(
                    "%d. %s - %d point%s\n",
                    prevRank,
                    topNames.get(i),
                    topPoints.get(i),
                    topPoints.get(i) != 1 ? "s" : "");
            if (message.length() + entry.length() > DISCORD_MAX_CHARS - 3) {
                break;
            }
            else {
                message.append(entry);
            }
        }
        message.append("```");
        
        //edit message for current month if it exists, otherwise make a new one
        if (dbc.hasRecentMessageContents(DISCORD_CHANNEL_MONTHLY)) {
            String dateString = dbc.getMostRecentMessageContents(DISCORD_CHANNEL_MONTHLY).split("\n", 2)[0];
            YearMonth now = YearMonth.now();
            YearMonth messageDate = YearMonth.parse(dateString, DateTimeFormatter.ofPattern("MMMM yyyy"));
            if (now.getMonth() == messageDate.getMonth() && now.getYear() == messageDate.getYear()) {
                dbc.editMostRecentMessage(DISCORD_CHANNEL_MONTHLY, message.toString());
                return;
            }
        }
        dbc.sendMessage(DISCORD_CHANNEL_MONTHLY, message.toString());
    }
    
    private void updateDiscordAllTimePoints() {
        ArrayList<Long> topScorers = leaderboard.getTopScorers();
        ArrayList<Integer> topPoints = new ArrayList<>();
        ArrayList<String> topNames = new ArrayList<>();
        for (Long topScorer : topScorers) {
            topPoints.add(leaderboard.getPoints(topScorer));
            topNames.add(leaderboard.getUsername(topScorer));
        }
        
        StringBuilder message = new StringBuilder();
        message.append("All-Time Points:\n```");
        
        //add entries until discord character limit is reached
        int prevPoints = -1;
        int prevRank = -1;
        for (int i = 0; i < topNames.size(); i++) {
            if (topPoints.get(i) != prevPoints) {
                prevRank = i + 1;
            }
            prevPoints = topPoints.get(i);
            String entry = String.format("%d. %s - %d points\n", prevRank, topNames.get(i), topPoints.get(i));
            if (message.length() + entry.length() > DISCORD_MAX_CHARS - 3) {
                break;
            }
            else {
                message.append(entry);
            }
        }
        message.append("```");
        
        if (dbc.hasRecentMessageContents(DISCORD_CHANNEL_ALL_TIME)) {
            dbc.editMostRecentMessage(DISCORD_CHANNEL_ALL_TIME, message.toString());
        }
        else {
            dbc.sendMessage(DISCORD_CHANNEL_ALL_TIME, message.toString());
        }
    }
    
    /**
     * Converts a {@link Badge} to a {@link String}
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
