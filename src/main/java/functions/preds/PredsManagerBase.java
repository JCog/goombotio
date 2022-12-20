package functions.preds;

import com.github.twitch4j.helix.domain.Moderator;
import com.github.twitch4j.helix.domain.User;
import database.DbManager;
import database.misc.PermanentVipsDb;
import database.misc.VipRaffleDb;
import database.preds.PredsLeaderboardDb;
import functions.DiscordBotController;
import util.TwitchApi;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import static java.lang.System.out;

public abstract class PredsManagerBase {
    private static final int DISCORD_MAX_CHARS = 2000;
    private static final String STOP_MESSAGE = "Predictions are up! Let's see how everyone did...";
    
    final DiscordBotController discord;
    final TwitchApi twitchApi;
    final DbManager dbManager;
    final VipRaffleDb vipRaffleDb;
    final PredsLeaderboardDb leaderboard;
    final String startMessage;
    final String answerRegex;

    boolean isEnabled;
    boolean waitingForAnswer;

    PredsManagerBase(
            TwitchApi twitchApi,
            DbManager dbManager,
            DiscordBotController discord,
            PredsLeaderboardDb leaderboard,
            String startMessage,
            String answerRegex
    ) {
        this.twitchApi = twitchApi;
        this.dbManager = dbManager;
        this.vipRaffleDb = dbManager.getVipRaffleDb();
        this.discord = discord;
        this.leaderboard = leaderboard;
        this.startMessage = startMessage;
        this.answerRegex = answerRegex;
    }

    /**
     * Returns true if there is an active !preds game
     *
     * @return enabled state
     */
    public boolean isActive() {
        return isEnabled;
    }

    /**
     * Returns true if the game is waiting on the correct answer
     *
     * @return waiting for answer state
     */
    public boolean isWaitingForAnswer() {
        return waitingForAnswer;
    }

    /**
     * Sets the game to an enabled state and sends a message to the chat to tell users to begin submitting predictions.
     */
    public void startGame() {
        isEnabled = true;
        twitchApi.channelAnnouncement(startMessage);
    }

    /**
     * Sets the game to a state where it's waiting for the correct answer and sends a message to the chat to let them
     * know to stop submitting predictions.
     */
    public void waitForAnswer() {
        waitingForAnswer = true;
        twitchApi.channelAnnouncement(STOP_MESSAGE);
    }
    
    // only used for verifying answer from broadcaster
    public String getAnswerRegex() {
        return answerRegex;
    }
    
    public static String buildMonthlyLeaderboardString(
            PredsLeaderboardDb leaderboard,
            PermanentVipsDb permanentVipsDb,
            TwitchApi twitchApi,
            User streamer
    ) {
        ArrayList<Long> topMonthlyScorers = leaderboard.getTopMonthlyScorers();
        ArrayList<Integer> topMonthlyPoints = new ArrayList<>();
        ArrayList<String> topMonthlyNames = new ArrayList<>();
        List<String> mods = twitchApi.getMods(streamer.getId()).stream().map(Moderator::getUserLogin).collect(Collectors.toList());
        HashSet<String> permanentVips = new HashSet<>(permanentVipsDb.getAllVipUserIds());
        
        for (Long topMonthlyScorer : topMonthlyScorers) {
            topMonthlyPoints.add(leaderboard.getMonthlyPoints(topMonthlyScorer));
            topMonthlyNames.add(leaderboard.getUsername(topMonthlyScorer));
        }
        
        int prevPoints = -1;
        int prevRank = -1;
        int ineligibleCount = 0;
        ArrayList<String> leaderboardStrings = new ArrayList<>();
        for (int i = 0; i < topMonthlyNames.size(); i++) {
            if (permanentVips.contains(topMonthlyNames.get(i).toLowerCase()) || mods.contains(topMonthlyNames.get(i).toLowerCase())) {
                ineligibleCount++;
            }
            if (topMonthlyPoints.get(i) != prevPoints) {
                prevRank = i + 1;
                if (prevRank > 5 + ineligibleCount) {
                    break;
                }
            }
            prevPoints = topMonthlyPoints.get(i);
            String name = topMonthlyNames.get(i);
            
            leaderboardStrings.add(String.format("%d. %s - %d", prevRank, name, prevPoints));
        }
        
        return "Monthly Leaderboard: " + String.join(", ", leaderboardStrings);
    }
    
    public abstract void submitPredictions(String answer);
    
    public abstract void makePredictionIfValid(String userId, String displayName, String message);
    
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    
    void updateDiscordPoints(String channel) {
        Thread thread = new Thread(() -> {
            if (leaderboard == null) {
                out.println("Leaderboard DB is null, ignoring attempt to update preds leaderboard in Discord.");
                return;
            }
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
                } else {
                    message.append(entry);
                }
            }
            message.append("```");
            
            if (discord.hasRecentMessageContents(channel)) {
                discord.editMostRecentMessage(channel, message.toString());
            } else {
                discord.sendMessage(channel, message.toString());
            }
            out.printf("%s - updated.\n", channel);
        });
        thread.start();
    }
}
