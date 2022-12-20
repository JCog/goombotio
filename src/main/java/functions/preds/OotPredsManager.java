package functions.preds;

import database.DbManager;
import database.preds.DampeRaceLeaderboardDb;
import functions.DiscordBotController;
import util.TwitchApi;

import java.util.ArrayList;
import java.util.HashMap;

public class OotPredsManager extends PredsManagerBase {
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
    
    private final HashMap<String, TimeGuess> guesses = new HashMap<>();
    private final DampeRaceLeaderboardDb dampeRaceLeaderboardDb;
    
    public OotPredsManager(DbManager dbManager, DiscordBotController discord, TwitchApi twitchApi) {
        super(
                twitchApi,
                dbManager,
                discord,
                null,
                START_MESSAGE,
                ANSWER_REGEX
        );
        this.dampeRaceLeaderboardDb = dbManager.getDampeRaceLeaderboardDb();
    }
    
    @Override
    public void submitPredictions(String answer) {
        Integer answerSeconds = guessToSeconds(answer);
        ArrayList<TimeGuess> winners = new ArrayList<>();
        for (TimeGuess guess : guesses.values()) {
            boolean isWinner = false;
            int newEntryCount;
            
            int secondsOff = Math.abs(guess.seconds - answerSeconds);
            switch (secondsOff) {
                case 0:
                    isWinner = true;
                    winners.add(guess);
                    dampeRaceLeaderboardDb.addWin(guess.userId, guess.displayName);
                    newEntryCount = REWARD_CORRECT;
                    break;
                case 1: newEntryCount = REWARD_1_OFF; break;
                case 2: newEntryCount = REWARD_2_OFF; break;
                default: newEntryCount = REWARD_PARTICIPATION; break;
            }
            vipRaffleDb.incrementEntryCount(guess.userId, newEntryCount);
            System.out.printf(
                    "+%d entries %sto %s%n",
                    newEntryCount,
                    isWinner ? "and a win " : "",
                    guess.displayName
            );
        }
    
        if (winners.isEmpty()) {
            twitchApi.channelMessage(
                    "Nobody guessed it. jcogThump Everybody who participated gets at least 1 raffle entry " +
                    "though! Use !raffle to check your updated entry count."
            );
        } else {
    
            StringBuilder winnerString = new StringBuilder();
            switch (winners.size()) {
                case 1:
                    winnerString.append("@").append(winners.get(0).displayName);
                    break;
                case 2:
                    winnerString.append(String.format(
                            "@%s and @%s",
                            winners.get(0).displayName,
                            winners.get(1).displayName
                    ));
                    break;
                default:
                    for (int i = 0; i < winners.size() - 1; i++) {
                        winnerString.append(String.format("@%s, ", winners.get(i).displayName));
                    }
                    winnerString.append(String.format("and @%s", winners.get(winners.size() - 1).displayName));
                    break;
            }
            twitchApi.channelMessage(String.format(
                    "Congrats to %s on guessing correctly! jcogChamp Use !raffle to check your updated entry count.",
                    winnerString
            ));
        }
        updateDiscordWins();
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
        guesses.put(userId, new TimeGuess(userId, displayName, userGuess));
    }
    
    private void updateDiscordWins() {
        ArrayList<DampeRaceLeaderboardDb.DampeRaceLbItem> winners = dampeRaceLeaderboardDb.getWinners();
        StringBuilder message = new StringBuilder();
        message.append("Dampe Race Prediction Wins:\n```");
        
        // add winners until discord character limit is reached
        int prevWins = -1;
        int prevRank = -1;
        for (int i = 0; i < winners.size(); i++) {
            DampeRaceLeaderboardDb.DampeRaceLbItem winner = winners.get(i);
            if (winner.getWinCount() != prevWins) {
                prevRank = i + 1;
            }
            prevWins = winner.getWinCount();
            String nextString = String.format(
                    "%d. %s - %d\n",
                    prevRank,
                    winner.getDisplayName(),
                    winner.getWinCount()
            );
            if (message.length() + nextString.length() > DISCORD_MAX_CHARS - 3) {
                break;
            }
            message.append(nextString);
        }
        message.append("```");

        if (discord.hasRecentMessageContents(DISCORD_CHANNEL)) {
            discord.editMostRecentMessage(DISCORD_CHANNEL, message.toString());
        } else {
            discord.sendMessage(DISCORD_CHANNEL, message.toString());
        }
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
    
    private static class TimeGuess {
    
        public final String userId;
        public final String displayName;
        public final int seconds;
        
        public TimeGuess(String userId, String displayName, int seconds) {
            this.userId = userId;
            this.displayName = displayName;
            this.seconds = seconds;
        }
        
        public String getUserId() {
            return userId;
        }
    }
}
