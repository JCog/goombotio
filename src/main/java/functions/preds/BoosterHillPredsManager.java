package functions.preds;

import com.github.twitch4j.helix.domain.Moderator;
import database.DbManager;
import database.preds.BoosterHillLeaderboardDb;
import functions.DiscordBotController;
import util.TwitchApi;

import java.util.*;
import java.util.stream.Collectors;

public class BoosterHillPredsManager extends PredsManagerBase {
    private static final String START_MESSAGE =
            "Get your predictions in! Guess in chat how many Flowers JCog will get during Booster Hill. The closer " +
            "you are, the more entries you'll earn for a chance at winning the monthly VIP raffle! Type !preds to " +
            "learn more.";
    private static final String ANSWER_REGEX = "(^[0-9]*)";
    
    private static final String DISCORD_CHANNEL = "booster-hill";
    private static final int REWARD_CORRECT = 20;
    private static final int REWARD_1_OFF = 5;
    private static final int REWARD_2_OFF = 2;
    private static final int REWARD_PARTICIPATION = 1;
    
    private final Map<String, FlowersGuess> guesses = new HashMap<>();
    private final BoosterHillLeaderboardDb boosterHillLeaderboardDb;
    
    public BoosterHillPredsManager(DbManager dbManager, DiscordBotController discord, TwitchApi twitchApi) {
        super(
                twitchApi,
                dbManager,
                discord,
                START_MESSAGE,
                ANSWER_REGEX
        );
        this.boosterHillLeaderboardDb = dbManager.getBoosterHillLeaderboardDb();
    }
    
    @Override
    public void submitPredictions(String answer) {
        Set<String> modIds = twitchApi.getMods(twitchApi.getStreamerUser().getId())
                .stream()
                .map(Moderator::getUserId)
                .collect(Collectors.toSet());
        
        List<FlowersGuess> winners = new ArrayList<>();
        for (Map.Entry<String, FlowersGuess> guess : guesses.entrySet()) {
            String userId = guess.getKey();
            String displayName = guess.getValue().displayName;
            int flowers = guess.getValue().flowers;
            
            boolean isWinner = false;
            int newEntryCount;
            int answerFlowers = Integer.parseInt(answer);
            
            int flowersOff = Math.abs(flowers - answerFlowers);
            switch (flowersOff) {
                case 0:
                    isWinner = true;
                    winners.add(guess.getValue());
                    boosterHillLeaderboardDb.addWin(userId, displayName);
                    newEntryCount = REWARD_CORRECT;
                    break;
                case 1: newEntryCount = REWARD_1_OFF; break;
                case 2: newEntryCount = REWARD_2_OFF; break;
                default: newEntryCount = REWARD_PARTICIPATION; break;
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
        
        updateDiscordLeaderboardWins(
                DISCORD_CHANNEL,
                "Booster Hill Prediction Wins:",
                boosterHillLeaderboardDb.getAllSortedWins()
        );
    }
    
    @Override
    public void makePredictionIfValid(String userId, String displayName, String message) {
        int userGuess;
        try {
            userGuess = Integer.parseInt(message);
        } catch (NumberFormatException e) {
            return;
        }
        
        if (guesses.containsKey(userId)) {
            System.out.printf("Replacing %s's guess with %d flowers.%n", displayName, userGuess);
        } else {
            System.out.printf("%s has guessed %d flowers.%n", displayName, userGuess);
        }
        guesses.put(userId, new FlowersGuess(displayName, userGuess));
    }
    
    private static class FlowersGuess {
        public final String displayName;
        public final int flowers;
        
        public FlowersGuess(String displayName, int flowers) {
            this.displayName = displayName;
            this.flowers = flowers;
        }
    }
}
