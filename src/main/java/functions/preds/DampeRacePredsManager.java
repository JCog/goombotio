package functions.preds;

import com.github.twitch4j.helix.domain.Moderator;
import database.preds.DampeRaceLeaderboardDb;
import util.CommonUtils;

import java.util.*;
import java.util.stream.Collectors;

public class DampeRacePredsManager extends PredsManagerBase {
    private static final String START_MESSAGE =
            "Get your predictions in! Guess in chat what the timer will say at the end of the Dampe race. The closer " +
            "you are, the more entries you'll earn for a chance at winning the monthly VIP raffle! Type !preds to " +
            "learn more.";
    private static final String ANSWER_REGEX = "(^[0-5][0-9])|(^[1-9][0-5][0-9])";
    
    private static final String DISCORD_CHANNEL = "dampe-race";
    private static final int REWARD_CORRECT = 15;
    private static final int REWARD_1_OFF = 5;
    private static final int REWARD_2_OFF = 2;
    private static final int REWARD_PARTICIPATION = 1;
    
    private record TimeGuess(String displayName, int seconds) {}
    
    private final Map<String, TimeGuess> guesses = new HashMap<>();
    private final DampeRaceLeaderboardDb dampeRaceLeaderboardDb;
    
    public DampeRacePredsManager(CommonUtils commonUtils) {
        super(commonUtils, START_MESSAGE, ANSWER_REGEX);
        dampeRaceLeaderboardDb = commonUtils.dbManager().getDampeRaceLeaderboardDb();
    }
    
    @Override
    public void submitPredictions(String answer) {
        Set<String> modIds = twitchApi.getMods(twitchApi.getStreamerUser().getId())
                .stream()
                .map(Moderator::getUserId)
                .collect(Collectors.toSet());
        
        Integer answerSeconds = guessToSeconds(answer);
        List<TimeGuess> winners = new ArrayList<>();
        for (Map.Entry<String, TimeGuess> guess : guesses.entrySet()) {
            String userId = guess.getKey();
            String displayName = guess.getValue().displayName();
            int seconds = guess.getValue().seconds();
            
            boolean isWinner = false;
            int newEntryCount;
            
            int secondsOff = Math.abs(seconds - answerSeconds);
            switch (secondsOff) {
                case 0 -> {
                    isWinner = true;
                    winners.add(guess.getValue());
                    dampeRaceLeaderboardDb.addWin(userId, displayName);
                    newEntryCount = REWARD_CORRECT;
                }
                case 1 -> newEntryCount = REWARD_1_OFF;
                case 2 -> newEntryCount = REWARD_2_OFF;
                default -> newEntryCount = REWARD_PARTICIPATION;
            }
            
            if (!modIds.contains(userId) && !vipDb.isPermanentVip(userId)) {
                vipRaffleDb.incrementEntryCount(userId, displayName, newEntryCount);
                System.out.printf("+%d entries %sto %s%n", newEntryCount, isWinner ? "and a win " : "", displayName);
            } else if (isWinner) {
                System.out.printf("No entries, but +1 win to %s%n", displayName);
            }
        }
    
        if (winners.isEmpty()) {
            twitchApi.channelMessage(
                    "Nobody guessed it. jcogThump Everybody who participated gets at least 1 raffle entry " +
                    "though! Use !raffle to check your updated entry count."
            );
        } else {
            StringBuilder winnerString = new StringBuilder();
            switch (winners.size()) {
                case 1 -> winnerString.append("@").append(winners.get(0).displayName());
                case 2 -> winnerString.append(String.format(
                        "@%s and @%s",
                        winners.get(0).displayName(),
                        winners.get(1).displayName()
                ));
                default -> {
                    for (int i = 0; i < winners.size() - 1; i++) {
                        winnerString.append(String.format("@%s, ", winners.get(i).displayName()));
                    }
                    winnerString.append(String.format("and @%s", winners.get(winners.size() - 1).displayName()));
                }
            }
            twitchApi.channelMessage(String.format(
                    "Congrats to %s on guessing correctly! jcogChamp Use !raffle to check your updated entry count.",
                    winnerString
            ));
        }
        
        updateDiscordLeaderboardWins(
                DISCORD_CHANNEL,
                "Dampe Race Prediction Wins:",
                dampeRaceLeaderboardDb.getAllSortedWins()
        );
    }
    
    @Override
    public void makePredictionIfValid(String userId, String displayName, String message) {
        Integer userGuess = guessToSeconds(message);
        if (userGuess == null) {
            return;
        }
        
        if (guesses.containsKey(userId)) {
            System.out.printf("Replacing %s's guess with %d seconds.%n", displayName, userGuess);
        } else {
            System.out.printf("%s has guessed %d seconds.%n", displayName, userGuess);
        }
        guesses.put(userId, new TimeGuess(displayName, userGuess));
    }
    
    private Integer guessToSeconds(String userMessage) {
        String guess = userMessage.replaceAll("[^0-9]", "");
        Integer totalSeconds = null;
        if (guess.matches("^[0-5][0-9]")) {
            // two-digit guess
            totalSeconds = Integer.parseInt(guess.substring(0, 2));
        } else if (guess.matches("^[1-9][0-5][0-9]")) {
            // three-digit guess
            int minutes = Integer.parseInt(guess.substring(0, 1));
            int seconds = Integer.parseInt(guess.substring(1));
            totalSeconds = minutes * 60 + seconds;
        }
        return totalSeconds;
    }
}
