package functions.preds;

import com.github.twitch4j.helix.domain.User;
import database.DbManager;
import functions.DiscordBotController;
import util.TwitchApi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static java.lang.System.out;

public class SunshinePredsManager extends PredsManagerBase {
    private static final String START_MESSAGE =
            "Get your predictions in! Guess what the timer will end at when JCog finishes Pianta 6. You get more " +
            "points the closer you are, plus a bonus if you're closest, and if you're closest and within half a " +
            "second, JCog will gift you a sub! Type !preds to learn more.";
    private static final String ANSWER_REGEX = "[0-9]{5}";
    private static final String DISCORD_POINTS_CHANNEL = "sms-preds-all-time";
    
    private static final int POINTS_CORRECT = 50;
    private static final int POINTS_1_SECOND = 15;
    private static final int POINTS_5_SECONDS = 5;
    private static final int POINTS_10_SECONDS = 1;
    private static final int POINTS_CLOSEST = 10;
    private static final int HUND_1_SECOND = 100;
    private static final int HUND_5_SECONDS = 5 * 100;
    private static final int HUND_10_SECONDS = 10 * 100;

    private final HashMap<Long,TimeGuess> predictionList = new HashMap<>();
    private final User streamer;

    public SunshinePredsManager(DbManager dbManager, DiscordBotController discord, TwitchApi twitchApi, User streamer) {
        super(
                twitchApi,
                dbManager,
                discord,
                dbManager.getSunshineTimerLeaderboardDb(),
                START_MESSAGE,
                ANSWER_REGEX
        );
        this.streamer = streamer;
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

        ArrayList<String> winners = getWinners(hundredths);
        StringBuilder message = new StringBuilder();
        if (winners.size() == 0) {
            ArrayList<TimeGuess> closestGuesses = getClosestGuesses(hundredths);
            if (closestGuesses.size() == 0) {
                message.append("Nobody guessed jcogREE");
            } else {
                String difference = formatDifference(hundredths, closestGuesses.get(0).hundredths);
                if (closestGuesses.size() == 1) {
                    message.append(String.format(
                            "Nobody won, but @%s was closest (+/- %ss)! jcogComfy",
                            closestGuesses.get(0).displayName,
                            difference
                    ));
                } else if (closestGuesses.size() == 2) {
                    message.append(String.format(
                            "Nobody won, but @%s and @%s were closest (+/- %ss)! jcogComfy",
                            closestGuesses.get(0).displayName,
                            closestGuesses.get(1).displayName,
                            difference
                    ));
                } else {
                    message.append("Nobody won, but ");
                    for (int i = 0; i < closestGuesses.size() - 1; i++) {
                        message.append("@").append(closestGuesses.get(i).displayName).append(", ");
                    }
                    message.append("and @")
                            .append(closestGuesses.get(closestGuesses.size() - 1).displayName);
                    message.append(String.format(" were closest (+/- %ss)! jcogComfy", difference));
                }
            }
        } else if (winners.size() == 1) {
            message.append(String.format(
                    "Congrats to @%s on guessing correctly! jcogChamp",
                    winners.get(0)
            ));
        } else if (winners.size() == 2) {
            message.append(String.format(
                    "Congrats to @%s and @%s on guessing correctly! jcogChamp",
                    winners.get(0),
                    winners.get(1)
            ));
        } else {
            message.append("Congrats to ");
            for (int i = 0; i < winners.size() - 1; i++) {
                message.append("@").append(winners.get(i)).append(", ");
            }
            message.append("and @").append(winners.get(winners.size() - 1));
            message.append(" on guessing correctly! jcogChamp");
        }
        message.append(" • ");
        message.append(buildMonthlyLeaderboardString(
                leaderboard,
                dbManager.getPermanentVipsDb(),
                twitchApi,
                streamer
        ));
    
        twitchApi.channelAnnouncement(String.format(
                "The correct answer is %s - %s",
                formatHundredths(hundredths),
                message
        ));
        updateDiscordPoints(DISCORD_POINTS_CHANNEL);
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

            if (predictionList.containsKey(Long.parseLong(userId))) {
                predictionList.remove(Long.parseLong(userId));
                out.printf("Replacing duplicate guess by %s%n", displayName);
            }
            predictionList.put(Long.parseLong(userId), new TimeGuess(userId, displayName, hundredths));
        }
    }
    
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private ArrayList<String> getWinners(int answer) {
        ArrayList<String> winners = new ArrayList<>();
        for (Map.Entry<Long,TimeGuess> longTimeGuessEntry : predictionList.entrySet()) {
            TimeGuess guess = longTimeGuessEntry.getValue();
            if (guess.hundredths == answer) {
                //exactly right
                winners.add(guess.displayName);
                leaderboard.addPointsAndWins(guess.userId, guess.displayName, POINTS_CORRECT, 1);
                out.printf(
                        "%s guessed exactly correct. Adding %d points and a win.%n",
                        guess.displayName,
                        POINTS_CORRECT
                );
            } else if (Math.abs(guess.hundredths - answer) < HUND_1_SECOND) {
                //off by less than a second
                leaderboard.addPoints(guess.userId, guess.displayName, POINTS_1_SECOND);
                out.printf(
                        "%s was within 1 second. Adding %d points%n",
                        guess.displayName,
                        POINTS_1_SECOND
                );
            } else if (Math.abs(guess.hundredths - answer) < HUND_5_SECONDS) {
                //off by less than 5 seconds
                leaderboard.addPoints(guess.userId, guess.displayName, POINTS_5_SECONDS);
                out.printf(
                        "%s was within 5 seconds. Adding %d points%n",
                        guess.displayName,
                        POINTS_5_SECONDS
                );
            } else if (Math.abs(guess.hundredths - answer) < HUND_10_SECONDS) {
                //off by less than 10 seconds
                leaderboard.addPoints(guess.userId, guess.displayName, POINTS_10_SECONDS);
                out.printf(
                        "%s was within 5 seconds. Adding %d points%n",
                        guess.displayName,
                        POINTS_10_SECONDS
                );
            }
        }
        return winners;
    }

    //returns the closest guess, multiple if there are ties
    private ArrayList<TimeGuess> getClosestGuesses(int answer) {
        int minDifference = -1;
        for (Map.Entry<Long,TimeGuess> longTimeGuessEntry : predictionList.entrySet()) {
            TimeGuess guess = longTimeGuessEntry.getValue();
            if (minDifference == -1) {
                minDifference = Math.abs(guess.hundredths - answer);
            } else {
                int difference = Math.abs(guess.hundredths - answer);
                if (difference < minDifference) {
                    minDifference = difference;
                }
            }
        }

        ArrayList<TimeGuess> output = new ArrayList<>();
        for (Map.Entry<Long,TimeGuess> longTimeGuessEntry : predictionList.entrySet()) {
            TimeGuess guess = longTimeGuessEntry.getValue();
            if (Math.abs(guess.hundredths - answer) == minDifference) {
                output.add(guess);
                leaderboard.addPoints(guess.userId, guess.displayName, POINTS_CLOSEST);
                out.printf(
                        "%s was the closest. Adding %d points%n",
                        guess.displayName,
                        POINTS_CLOSEST
                );
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
        public final String userId;
        public final String displayName;
        public final int hundredths;

        public TimeGuess(String userId, String displayName, int hundredths) {
            this.userId = userId;
            this.displayName = displayName;
            this.hundredths = hundredths;
        }
    }
}
