package Functions.Preds;

import Database.Preds.PredsLeaderboard;
import Database.Preds.SunshineTimerLeaderboard;
import Util.TwirkInterface;
import com.gikk.twirk.types.users.TwitchUser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static java.lang.System.out;

public class SunshinePredsManager extends PredsManagerBase {
    private static final String DISCORD_CHANNEL_MONTHLY = "sms-preds-monthly";
    private static final String DISCORD_CHANNEL_ALL_TIME = "sms-preds-all-time";
    private static final String START_MESSAGE = "/me Get your predictions in! Guess what the timer will end at when JCog finishes Pianta 6. You get more points the closer you are, plus a bonus if you're closest, and if you get it exactly right JCog will gift you a sub! Type !preds to learn more.";
    private static final int POINTS_CORRECT = 50;
    private static final int POINTS_1_SECOND = 15;
    private static final int POINTS_5_SECONDS = 5;
    private static final int POINTS_10_SECONDS = 1;
    private static final int POINTS_CLOSEST = 10;
    private static final int HUND_1_SECOND = 100;
    private static final int HUND_5_SECONDS = 5 * 100;
    private static final int HUND_10_SECONDS = 10 * 100;
    
    private final HashMap<Long, TimeGuess> predictionList = new HashMap<>();
    
    public SunshinePredsManager(TwirkInterface twirk) {
        super(twirk);
    }
    
    @Override
    protected PredsLeaderboard getLeaderboardType() {
        return SunshineTimerLeaderboard.getInstance();
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
        return START_MESSAGE;
    }
    
    public boolean isActive() {
        return enabled;
    }
    
    public boolean isWaitingForAnswer() {
        return waitingForAnswer;
    }
    
    //submit the correct answer, calculate points, end game
    @Override
    public void submitPredictions(String answer) {
        int outcome = Integer.parseInt(answer);
        int minutes = outcome / 10000;
        outcome -= minutes * 10000;
        int seconds = outcome / 100;
        outcome -= seconds * 100;
    
        int hundredths = outcome + (seconds * 100) + (minutes * 60 * 100);
        enabled = false;
        waitingForAnswer = false;
    
        ArrayList<String> winners = getWinners(hundredths);
        StringBuilder message = new StringBuilder();
        if (winners.size() == 0) {
            ArrayList<TimeGuess> closestGuesses = getClosestGuesses(hundredths);
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
                formatHundredths(hundredths),
                message.toString()
        ));
        updateDiscordMonthlyPoints();
        updateDiscordAllTimePoints();
    }
    
    @Override
    public String getAnswerRegex() {
        return "[0-9]{5}";
    }
    
    @Override
    public void makePredictionIfValid(TwitchUser user, String message) {
        String guess = message.replaceAll("[^0-9]", "");
        if (guess.length() == 5) {
            int secondDigit = Character.getNumericValue(guess.charAt(1));
            if (secondDigit > 5) {
                return;
            }
            int minutes = Character.getNumericValue(guess.charAt(0));
            int seconds = Integer.parseInt(guess.substring(1, 3));
            int hundredths = Integer.parseInt(guess.substring(3, 5)) + (seconds * 100) + (minutes * 60 * 100);
            System.out.println(String.format("%s has predicted %d hundredths", user.getDisplayName(), hundredths));
    
            if (predictionList.containsKey(user.getUserID())) {
                predictionList.remove(user.getUserID());
                out.println(String.format("Replacing duplicate guess by %s", user.getDisplayName()));
            }
            predictionList.put(user.getUserID(), new TimeGuess(user, hundredths));
        }
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
