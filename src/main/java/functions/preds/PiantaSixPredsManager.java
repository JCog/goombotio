package functions.preds;

import database.preds.PiantaSixLeaderboardDb;
import util.CommonUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.lang.System.out;

public class PiantaSixPredsManager extends PredsManagerBase {
    private static final String START_MESSAGE =
            "Get your predictions in! Guess what the timer will end at when JCog finishes Pianta 6. You get more " +
            "points the closer you are, plus a bonus if you're closest, and if you're closest and within half a " +
            "second, JCog will gift you a sub! Type !preds to learn more.";
    private static final String ANSWER_REGEX = "[0-9]{5}";
    
    private static final String DISCORD_CHANNEL = "pianta-6";
    private static final int POINTS_CORRECT = 50;
    private static final int POINTS_1_SECOND = 15;
    private static final int POINTS_5_SECONDS = 5;
    private static final int POINTS_10_SECONDS = 1;
    private static final int POINTS_CLOSEST = 10;
    private static final int HUND_1_SECOND = 100;
    private static final int HUND_5_SECONDS = 5 * 100;
    private static final int HUND_10_SECONDS = 10 * 100;

    private final Map<String,TimeGuess> predictionList = new HashMap<>();
    private final PiantaSixLeaderboardDb piantaSixLeaderboardDb;

    public PiantaSixPredsManager(CommonUtils commonUtils) {
        super(commonUtils, START_MESSAGE, ANSWER_REGEX);
        piantaSixLeaderboardDb = commonUtils.getDbManager().getPiantaSixLeaderboardDb();
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
        isEnabled = false;
        waitingForAnswer = false;

        List<String> winners = getWinners(hundredths);
        StringBuilder message = new StringBuilder();
        switch (winners.size()) {
            case 0:
                List<TimeGuess> closestGuesses = getClosestGuesses(hundredths);
                if (closestGuesses.size() == 0) {
                    message.append("Nobody guessed jcogRage");
                } else {
                    String difference = formatDifference(hundredths, closestGuesses.get(0).hundredths);
                    switch (closestGuesses.size()) {
                        case 1:
                            message.append(String.format(
                                    "Nobody won, but @%s was closest (+/- %ss)! jcogComfy",
                                    closestGuesses.get(0).displayName,
                                    difference
                            ));
                            break;
                        case 2:
                            message.append(String.format(
                                    "Nobody won, but @%s and @%s were closest (+/- %ss)! jcogComfy",
                                    closestGuesses.get(0).displayName,
                                    closestGuesses.get(1).displayName,
                                    difference
                            ));
                            break;
                        default:
                            message.append("Nobody won, but ");
                            for (int i = 0; i < closestGuesses.size() - 1; i++) {
                                message.append("@").append(closestGuesses.get(i).displayName).append(", ");
                            }
                            message.append("and @")
                                    .append(closestGuesses.get(closestGuesses.size() - 1).displayName);
                            message.append(String.format(" were closest (+/- %ss)! jcogComfy", difference));
                            break;
                    }
                }
                break;
            case 1:
                message.append(String.format(
                        "Congrats to @%s on guessing correctly! jcogChamp",
                        winners.get(0)
                ));
                break;
            case 2:
                message.append(String.format(
                        "Congrats to @%s and @%s on guessing correctly! jcogChamp",
                        winners.get(0),
                        winners.get(1)
                ));
                break;
            default:
                message.append("Congrats to ");
                for (int i = 0; i < winners.size() - 1; i++) {
                    message.append("@").append(winners.get(i)).append(", ");
                }
                message.append("and @").append(winners.get(winners.size() - 1));
                message.append(" on guessing correctly! jcogChamp");
                break;
        }
        message.append(" Use !raffle to check your updated entry count.");
    
        twitchApi.channelAnnouncement(String.format(
                "The correct answer is %s - %s",
                formatHundredths(hundredths),
                message
        ));
    
        updateDiscordLeaderboardPoints(
                DISCORD_CHANNEL,
                "Pianta 6 Prediction Points:",
                piantaSixLeaderboardDb.getAllSortedPoints()
        );
    }

    @Override
    public void makePredictionIfValid(String userId, String displayName, String message) {
        String guess = message.replaceAll("[^0-9]", "");
        if (guess.length() == 5) {
            int secondDigit = Character.getNumericValue(guess.charAt(1));
            if (secondDigit > 5) {
                return;
            }
            int minutes = Character.getNumericValue(guess.charAt(0));
            int seconds = Integer.parseInt(guess.substring(1, 3));
            int hundredths = Integer.parseInt(guess.substring(3, 5)) + (seconds * 100) + (minutes * 60 * 100);
            System.out.printf("%s has predicted %d hundredths%n", displayName, hundredths);

            if (predictionList.containsKey(userId)) {
                out.printf("Replacing duplicate guess by %s%n", displayName);
            }
            predictionList.put(userId, new TimeGuess(displayName, hundredths));
        }
    }
    
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private List<String> getWinners(int answer) {
        List<String> winners = new ArrayList<>();
        for (Map.Entry<String,TimeGuess> guess : predictionList.entrySet()) {
            String userId = guess.getKey();
            String displayName = guess.getValue().displayName;
            
            int difference = Math.abs(guess.getValue().hundredths - answer);
            int pointsToAdd = 0;
            int secondsWithin = Integer.MAX_VALUE;
            if (difference == 0) {
                secondsWithin = 0;
                pointsToAdd = POINTS_CORRECT;
            } else if (difference < HUND_1_SECOND) {
                secondsWithin = 1;
                pointsToAdd = POINTS_1_SECOND;
            } else if (difference < HUND_5_SECONDS) {
                secondsWithin = 1;
                pointsToAdd = POINTS_5_SECONDS;
            } else if (difference < HUND_10_SECONDS) {
                secondsWithin = 10;
                pointsToAdd = POINTS_10_SECONDS;
            }
            
            if (pointsToAdd != 0) {
                piantaSixLeaderboardDb.addPoints(userId, displayName, pointsToAdd);
                if (secondsWithin == 0) {
                    winners.add(displayName);
                    piantaSixLeaderboardDb.addWin(userId, displayName);
                    out.printf(
                            "%s guessed exactly correct. Adding %d points and a win.%n",
                            displayName,
                            POINTS_CORRECT
                    );
                } else {
                    out.printf(
                            "%s was within %d seconds. Adding %d points%n",
                            displayName,
                            secondsWithin,
                            pointsToAdd
                    );
                }
            }
        }
        return winners;
    }

    //returns the closest guess, multiple if there are ties
    private List<TimeGuess> getClosestGuesses(int answer) {
        int minDifference = Integer.MAX_VALUE;
        for (TimeGuess guess : predictionList.values()) {
            minDifference = Integer.min(Math.abs(guess.hundredths - answer), minDifference);
        }

        List<TimeGuess> output = new ArrayList<>();
        for (Map.Entry<String,TimeGuess> guess : predictionList.entrySet()) {
            String userId = guess.getKey();
            String displayName = guess.getValue().displayName;
            int hundredths = guess.getValue().hundredths;
            
            if (Math.abs(hundredths - answer) == minDifference) {
                output.add(guess.getValue());
                piantaSixLeaderboardDb.addPoints(userId, displayName, POINTS_CLOSEST);
                out.printf("%s was the closest. Adding %d additional points%n", displayName, POINTS_CLOSEST);
            }
        }
        return output;
    }

    //takes in hundredths, outputs seconds e.g. 5.02
    private String formatDifference(int answer, int guess) {
        int difference = Math.abs(answer - guess);
        int seconds = difference / 100;
        int hundredths = difference % 100;
        return String.format("%d.%02d", seconds, hundredths);
    }

    private String formatHundredths(int hundredths) {
        int minutes = hundredths / (100 * 60);
        hundredths -= minutes * (100 * 60);
        int seconds = hundredths / 100;
        hundredths -= seconds * 100;
        return String.format("%d:%02d:%02d", minutes, seconds, hundredths);
    }

    private static class TimeGuess {
        public final String displayName;
        public final int hundredths;

        public TimeGuess(String displayName, int hundredths) {
            this.displayName = displayName;
            this.hundredths = hundredths;
        }
    }
}
