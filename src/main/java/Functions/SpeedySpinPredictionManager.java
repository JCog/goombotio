package Functions;

import Listeners.SpeedySpinPredictionListener;
import com.gikk.twirk.Twirk;
import com.gikk.twirk.types.users.TwitchUser;

import java.util.*;

public class SpeedySpinPredictionManager {

    private final Twirk twirk;
    private SpeedySpinPredictionListener sspListener;
    public enum Badge {
        BAD_SPIN1,
        BAD_SPIN2,
        BAD_SPIN3,
        SPOODLY_SPUN
    }

    private HashMap<TwitchUser, ArrayList<Badge>> predictionList;
    private boolean enabled;

    public SpeedySpinPredictionManager(Twirk twirk) {
        this.twirk = twirk;
        enabled = false;
        predictionList = new HashMap<>();
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
                "BadSpin3 or SpoodlySpun to guess the badge shop!");
    }

    public void stop(Badge one, Badge two, Badge three) {
        enabled = false;
        twirk.removeIrcListener(sspListener);

        ArrayList<String> winners = getWinners(one, two, three);
        String message;
        if (winners.size() == 0) {
            message = "Nobody guessed it though. pepeHands";
        }
        else if (winners.size() == 1) {
            message = String.format("Congrats to %s on guessing correctly! PogChamp", winners.get(0));
        }
        else if (winners.size() == 2) {
            message = String.format("Congrats to %s and %s on guessing correctly! PogChamp",winners.get(0), winners.get(1));
        }
        else {
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < winners.size() - 1; i++) {
                builder.append(winners.get(i) + ", ");
            }
            builder.append("and " + winners.get(winners.size() - 1));
            message = builder.toString();
        }

        twirk.channelMessage(String.format("/me Predictions are up! The correct answer was %s %s %s . %s",
                badgeToString(one), badgeToString(two), badgeToString(three), message));
    }

    public boolean isEnabled() {
        return enabled;
    }



    private ArrayList<String> getWinners(Badge one, Badge two, Badge three) {
        ArrayList<String> winners = new ArrayList<>();
        for (Map.Entry<TwitchUser, ArrayList<Badge>> pred : predictionList.entrySet()) {
            if (pred.getValue().get(0) == one && pred.getValue().get(1) == two && pred.getValue().get(2) == three) {
                winners.add(pred.getKey().getDisplayName());
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
