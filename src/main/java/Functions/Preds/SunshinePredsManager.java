package Functions.Preds;

import Database.Preds.SunshineTimerLeaderboard;
import Util.TwirkInterface;
import com.gikk.twirk.types.users.TwitchUser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static java.lang.System.out;

public class SunshinePredsManager {
    private static final String START_MESSAGE = "starting preds";
    private static final String STOP_MESSAGE = "waiting for correct answer";
    private static final int POINTS_CORRECT = 50;
    private static final int POINTS_1_SECOND = 10;
    private static final int POINTS_5_SECONDS = 5;
    private static final int POINTS_10_SECONDS = 1;
    private static final int POINTS_CLOSEST = 10;
    private static final int HUND_1_SECOND = 100;
    private static final int HUND_5_SECONDS = 5 * 100;
    private static final int HUND_10_SECONDS = 10 * 100;
    
    private final SunshineTimerLeaderboard leaderboard = SunshineTimerLeaderboard.getInstance();
    private final HashMap<Long, TimeGuess> predictionList = new HashMap<>();
    
    private final TwirkInterface twirk;
    
    private boolean enabled;
    private boolean waitingForAnswer;
    
    public SunshinePredsManager(TwirkInterface twirk) {
        this.twirk = twirk;
        enabled = false;
        waitingForAnswer = false;
    }
    
    public void makePrediction(TwitchUser user, int hundredths) {
        if (predictionList.containsKey(user.getUserID())) {
            predictionList.remove(user.getUserID());
            out.println(String.format("Replacing duplicate guess by %s", user.getDisplayName()));
        }
        predictionList.put(user.getUserID(), new TimeGuess(user, hundredths));
    }
    
    //start listening for preds
    public void start() {
        enabled = true;
        twirk.channelMessage(START_MESSAGE);
    }
    
    //stop listening for preds and wait for the correct answer
    public void stop() {
        waitingForAnswer = true;
        twirk.channelMessage(STOP_MESSAGE);
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public boolean isWaitingForAnswer() {
        return waitingForAnswer;
    }
    
    //submit the correct answer, calculate points, end game
    public void submitPredictions(int answer) {
        enabled = false;
        waitingForAnswer = false;
    
        ArrayList<String> winners = getWinners(answer);
        StringBuilder message = new StringBuilder();
        if (winners.size() == 0) {
            ArrayList<TimeGuess> closestGuesses = getClosestGuesses(answer);
            if (closestGuesses.size() == 0) {
                message.append("Nobody guessed jcogREE");
            }
            else if (closestGuesses.size() == 1) {
                message.append(String.format(
                        "Nobody won, but @%s was closest! jcogComfy",
                        closestGuesses.get(0).twitchUser.getDisplayName()
                ));
            }
            else if (closestGuesses.size() == 2) {
                message.append(String.format(
                        "Nobody won, but @%s and @%s were closest! jcogComfy",
                        closestGuesses.get(0).twitchUser.getDisplayName(),
                        closestGuesses.get(1).twitchUser.getDisplayName()
                ));
            }
            else {
                message.append("Nobody won, but ");
                for (int i = 0; i < closestGuesses.size() - 1; i++) {
                    message.append("@").append(closestGuesses.get(i).twitchUser.getDisplayName()).append(", ");
                }
                message.append("and @").append(closestGuesses.get(closestGuesses.size() - 1).twitchUser.getDisplayName());
                message.append(" were closest! jcogComfy");
            }
        }
        else if (winners.size() == 1) {
            message.append(String.format(
                    "Congrats to @%s on guessing correctly! jcogChamp",
                    winners.get(0)
            ));
        }
        else if (winners.size() == 2) {
            message.append(String.format(
                    "Congrats to @%s and @%s on guessing correctly! jcogChamp",
                    winners.get(0),
                    winners.get(1)
            ));
        }
        else {
            message.append("Congrats to ");
            for (int i = 0; i < winners.size() - 1; i++) {
                message.append("@").append(winners.get(i)).append(", ");
            }
            message.append("and @").append(winners.get(winners.size() - 1));
            message.append(" on guessing correctly! jcogChamp");
        }
        
        twirk.channelMessage(String.format(
                "/me The correct answer is %s - %s",
                formatHundredths(answer),
                message.toString()
        ));
    }
    
    private ArrayList<String> getWinners(int answer) {
        ArrayList<String> winners = new ArrayList<>();
        for (Map.Entry<Long, TimeGuess> longTimeGuessEntry : predictionList.entrySet()) {
            TimeGuess guess = longTimeGuessEntry.getValue();
            if (guess.hundredths == answer) {
                //exactly right
                winners.add(guess.twitchUser.getDisplayName());
                leaderboard.addPointsAndWins(guess.twitchUser, POINTS_CORRECT, 1);
                out.println(String.format(
                        "%s guessed exactly correct. Adding %d points and a win.",
                        guess.twitchUser.getDisplayName(),
                        POINTS_CORRECT
                ));
            }
            else if (Math.abs(guess.hundredths - answer) < HUND_1_SECOND) {
                //off by less than a second
                leaderboard.addPoints(guess.twitchUser, POINTS_1_SECOND);
                out.println(String.format(
                        "%s was within 1 second. Adding %d points",
                        guess.twitchUser.getDisplayName(),
                        POINTS_1_SECOND
                ));
            }
            else if (Math.abs(guess.hundredths - answer) < HUND_5_SECONDS) {
                //off by less than 5 seconds
                leaderboard.addPoints(guess.twitchUser, POINTS_5_SECONDS);
                out.println(String.format(
                        "%s was within 5 seconds. Adding %d points",
                        guess.twitchUser.getDisplayName(),
                        POINTS_5_SECONDS
                ));
            }
            else if (Math.abs(guess.hundredths - answer) < HUND_10_SECONDS) {
                //off by less than 10 seconds
                leaderboard.addPoints(guess.twitchUser, POINTS_10_SECONDS);
                out.println(String.format(
                        "%s was within 5 seconds. Adding %d points",
                        guess.twitchUser.getDisplayName(),
                        POINTS_10_SECONDS
                ));
            }
        }
        return winners;
    }
    
    //returns the closest guess, multiple if there are ties
    private ArrayList<TimeGuess> getClosestGuesses(int answer) {
        int minDifference = -1;
        for (Map.Entry<Long, TimeGuess> longTimeGuessEntry : predictionList.entrySet()) {
            TimeGuess guess = longTimeGuessEntry.getValue();
            if (minDifference == -1) {
                minDifference = Math.abs(guess.hundredths - answer);
            }
            else {
                int difference = Math.abs(guess.hundredths - answer);
                if (difference < minDifference) {
                    minDifference = difference;
                }
            }
        }
    
        ArrayList<TimeGuess> output = new ArrayList<>();
        for (Map.Entry<Long, TimeGuess> longTimeGuessEntry : predictionList.entrySet()) {
            TimeGuess guess = longTimeGuessEntry.getValue();
            if (Math.abs(guess.hundredths - answer) == minDifference) {
                output.add(guess);
                leaderboard.addPoints(guess.twitchUser, POINTS_CLOSEST);
                out.println(String.format(
                        "%s was the closest. Adding %d points",
                        guess.twitchUser.getDisplayName(),
                        POINTS_CLOSEST
                ));
            }
        }
        return output;
    }
    
    private String formatHundredths(int hundredths) {
        int minutes = hundredths / (100 * 60);
        hundredths -= minutes * (100 * 60);
        int seconds = hundredths / 100;
        hundredths -= seconds * 100;
        return String.format("%d:%02d:%02d", minutes, seconds, hundredths);
    }
    
    private static class TimeGuess {
        public TwitchUser twitchUser;
        public int hundredths;
        
        public TimeGuess(TwitchUser twitchUser, int hundredths) {
            this.twitchUser = twitchUser;
            this.hundredths = hundredths;
        }
    }
}
