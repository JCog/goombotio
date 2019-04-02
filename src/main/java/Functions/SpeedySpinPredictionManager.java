package Functions;

import Listeners.SpeedySpinPredictionListener;
import Util.Database.SpeedySpinLeaderboard;
import com.gikk.twirk.Twirk;
import com.gikk.twirk.types.users.TwitchUser;

import java.util.*;

import static java.lang.System.out;

public class SpeedySpinPredictionManager {

    private final int POINTS_3 = 20;
    private final int POINTS_2 = 5;
    private final int POINTS_1 = 1;
    private final Twirk twirk;
    private SpeedySpinPredictionListener sspListener;
    private SpeedySpinLeaderboard leaderboard;
    public enum Badge {
        BAD_SPIN1,
        BAD_SPIN2,
        BAD_SPIN3,
        SPOODLY_SPUN
    }

    private HashMap<TwitchUser, ArrayList<Badge>> predictionList;
    private boolean enabled;
    private boolean waitingForAnswer;

    public SpeedySpinPredictionManager(Twirk twirk) {
        this.twirk = twirk;
        enabled = false;
        waitingForAnswer = false;
        predictionList = new HashMap<>();
        leaderboard = new SpeedySpinLeaderboard();
    }

    public void makePrediction(TwitchUser user, Badge first, Badge second, Badge third) {
        if (enabled) {
            ArrayList<Badge> prediction = new ArrayList<>();
            prediction.add(first);
            prediction.add(second);
            prediction.add(third);
            predictionList.put(user, prediction);
        }
    }


    public void start() {
        enabled = true;
        twirk.addIrcListener(sspListener = new SpeedySpinPredictionListener(this));
        twirk.channelMessage("/me Get your predictions in! Send a message with three of either BadSpin1 BadSpin2 " +
                "BadSpin3 or SpoodlySpun to guess the badge shop! Type !preds to learn more.");
    }

    public void stop() {
        waitingForAnswer = true;
        twirk.removeIrcListener(sspListener);
        twirk.channelMessage("/me Predictions are up! Let's see how everyone did...");
    }

    public void submitPredictions(Badge one, Badge two, Badge three) {
        enabled = false;
        waitingForAnswer = false;

        ArrayList<String> winners = getWinners(one, two, three);
        String message;
        if (winners.size() == 0) {
            message = "Nobody guessed it. BibleThump Hopefully you got some points, though!";
        }
        else if (winners.size() == 1) {
            message = String.format("Congrats to @%s on guessing correctly! PogChamp", winners.get(0));
        }
        else if (winners.size() == 2) {
            message = String.format("Congrats to @%s and @%s on guessing correctly! PogChamp", winners.get(0), winners.get(1));
        }
        else {
            StringBuilder builder = new StringBuilder();
            builder.append("Congrats to ");
            for (int i = 0; i < winners.size() - 1; i++) {
                builder.append("@").append(winners.get(i)).append(", ");
            }
            builder.append("and @").append(winners.get(winners.size() - 1));
            builder.append(" on guessing correctly! PogChamp");
            message = builder.toString();
        }

        twirk.channelMessage(String.format("/me The correct answer was %s %s %s - %s",
                badgeToString(one), badgeToString(two), badgeToString(three), message));
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isWaitingForAnswer() {
        return waitingForAnswer;
    }



    private ArrayList<String> getWinners(Badge leftAnswer, Badge middleAnswer, Badge rightAnswer) {
        ArrayList<String> winners = new ArrayList<>();
        for (Map.Entry<TwitchUser, ArrayList<Badge>> pred : predictionList.entrySet()) {
            TwitchUser user = pred.getKey();
            Badge leftGuess = pred.getValue().get(0);
            Badge middleGuess = pred.getValue().get(1);
            Badge rightGuess = pred.getValue().get(2);

            if (leftGuess == leftAnswer && middleGuess == middleAnswer && rightGuess == rightAnswer) {
                winners.add(pred.getKey().getDisplayName());
                leaderboard.addPointsAndWins(user, POINTS_3, 1);
                out.println(String.format("%s guessed 3 correctly. Adding %d points and a win.", user.getDisplayName(), POINTS_3));
            }
            else if ((leftGuess == leftAnswer && middleGuess == middleAnswer) ||
                    (leftGuess == leftAnswer && rightGuess == rightAnswer) ||
                    (middleGuess == middleAnswer && rightGuess == rightAnswer)) {
                leaderboard.addPoints(user, POINTS_2);
                out.println(String.format("%s guessed 2 correctly. Adding %d points.", user.getDisplayName(), POINTS_2));
            }
            else if (leftGuess == leftAnswer || middleGuess == middleAnswer || rightGuess == middleAnswer) {
                leaderboard.addPoints(user, POINTS_1);
                out.println(String.format("%s guessed 1 correctly. Adding %d point.", user.getDisplayName(), POINTS_1));
            }
        }
        return winners;
    }

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

    public static Badge stringToBadge(String badge) {
        switch (badge) {
            case "BadSpin1":
                return Badge.BAD_SPIN1;
            case "BadSpin2":
                return Badge.BAD_SPIN2;
            case "BadSpin3":
                return Badge.BAD_SPIN3;
            case "SpoodlySpun":
                return Badge.SPOODLY_SPUN;
            default:
                return null;
        }
    }
}
