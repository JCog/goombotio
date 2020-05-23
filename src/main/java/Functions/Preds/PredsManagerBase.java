package Functions.Preds;

import Database.Preds.PredsLeaderboard;
import Functions.DiscordBotController;
import Util.TwirkInterface;
import com.gikk.twirk.types.users.TwitchUser;

import java.text.SimpleDateFormat;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;

public abstract class PredsManagerBase {
    private static final int DISCORD_MAX_CHARS = 2000;
    private static final String STOP_MESSAGE = "/me Predictions are up! Let's see how everyone did...";
    
    protected final String DISCORD_CHANNEL_MONTHLY = getMonthlyChannelName();
    protected final String DISCORD_CHANNEL_ALL_TIME = getAllTimeChannelName();
    protected final String START_MESSAGE = getStartMessage();
    
    
    protected final PredsLeaderboard leaderboard;
    
    private final DiscordBotController discord = DiscordBotController.getInstance();
    
    protected final TwirkInterface twirk;
    
    protected boolean enabled;
    protected boolean waitingForAnswer;
    
    protected PredsManagerBase(TwirkInterface twirk) {
        leaderboard = getLeaderboardType();
        this.twirk = twirk;
    }
    
    /**
     * Returns true if there is an active !preds game
     * @return enabled state
     */
    public boolean isActive() {
        return enabled;
    }
    
    /**
     * Returns true if the game is waiting on the correct answer
     * @return waiting for answer state
     */
    public boolean isWaitingForAnswer() {
        return waitingForAnswer;
    }
    
    /**
     * Sets the game to an enabled state and sends a message to the chat to tell users to begin submitting predictions.
     */
    public void startGame() {
        enabled = true;
        twirk.channelMessage(START_MESSAGE);
    }
    
    /**
     * Sets the game to a state where it's waiting for the correct answer and sends a message to the chat to let them
     * know to stop submitting predictions.
     */
    public void waitForAnswer() {
        waitingForAnswer = true;
        twirk.channelMessage(STOP_MESSAGE);
    }
    
    public void endGame() {
        waitingForAnswer = false;
        enabled = false;
    }
    
    public abstract void submitPredictions(String answer);
    
    public abstract String getAnswerRegex();
    
    public abstract void makePredictionIfValid(TwitchUser user, String message);
    
    protected void updateDiscordMonthlyPoints() {
        ArrayList<Long> topScorers = leaderboard.getTopMonthlyScorers();
        ArrayList<Integer> topPoints = new ArrayList<>();
        ArrayList<String> topNames = new ArrayList<>();
        for (Long topScorer : topScorers) {
            topPoints.add(leaderboard.getMonthlyPoints(topScorer));
            topNames.add(leaderboard.getUsername(topScorer));
        }
        
        StringBuilder message = new StringBuilder();
        SimpleDateFormat sdf = new SimpleDateFormat("MMMM yyyy");
        message.append(sdf.format(new Date()));
        message.append("\n```");
        
        //add entries until discord character limit is reached
        int prevPoints = -1;
        int prevRank = -1;
        for (int i = 0; i < topNames.size(); i++) {
            if (topPoints.get(i) != prevPoints) {
                prevRank = i + 1;
            }
            prevPoints = topPoints.get(i);
            String entry = String.format(
                    "%d. %s - %d point%s\n",
                    prevRank,
                    topNames.get(i),
                    topPoints.get(i),
                    topPoints.get(i) != 1 ? "s" : "");
            if (message.length() + entry.length() > DISCORD_MAX_CHARS - 3) {
                break;
            }
            else {
                message.append(entry);
            }
        }
        message.append("```");
        
        //edit message for current month if it exists, otherwise make a new one
        if (discord.hasRecentMessageContents(DISCORD_CHANNEL_MONTHLY)) {
            String dateString = discord.getMostRecentMessageContents(DISCORD_CHANNEL_MONTHLY).split("\n", 2)[0];
            YearMonth now = YearMonth.now();
            YearMonth messageDate = YearMonth.parse(dateString, DateTimeFormatter.ofPattern("MMMM yyyy"));
            if (now.getMonth() == messageDate.getMonth() && now.getYear() == messageDate.getYear()) {
                discord.editMostRecentMessage(DISCORD_CHANNEL_MONTHLY, message.toString());
                return;
            }
        }
        discord.sendMessage(DISCORD_CHANNEL_MONTHLY, message.toString());
    }
    
    protected void updateDiscordAllTimePoints() {
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
            }
            else {
                message.append(entry);
            }
        }
        message.append("```");
        
        if (discord.hasRecentMessageContents(DISCORD_CHANNEL_ALL_TIME)) {
            discord.editMostRecentMessage(DISCORD_CHANNEL_ALL_TIME, message.toString());
        }
        else {
            discord.sendMessage(DISCORD_CHANNEL_ALL_TIME, message.toString());
        }
    }
    
    protected String buildMonthlyLeaderboardString() {
        ArrayList<Long> topMonthlyScorers = leaderboard.getTopMonthlyScorers();
        ArrayList<Integer> topMonthlyPoints = new ArrayList<>();
        ArrayList<String> topMonthlyNames = new ArrayList<>();
        
        for (Long topMonthlyScorer : topMonthlyScorers) {
            topMonthlyPoints.add(leaderboard.getMonthlyPoints(topMonthlyScorer));
            topMonthlyNames.add(leaderboard.getUsername(topMonthlyScorer));
        }
        
        int prevPoints = -1;
        int prevRank = -1;
        ArrayList<String> leaderboardStrings = new ArrayList<>();
        for (int i = 0; i < topMonthlyNames.size(); i++) {
            if (topMonthlyPoints.get(i) != prevPoints) {
                prevRank = i + 1;
                if (prevRank > 5) {
                    break;
                }
            }
            prevPoints = topMonthlyPoints.get(i);
            String name = topMonthlyNames.get(i);
            
            leaderboardStrings.add(String.format("%d. %s - %d", prevRank, name, prevPoints));
        }
        
        return "Monthly Leaderboard: " + String.join(", ", leaderboardStrings);
    }
    
    protected abstract PredsLeaderboard getLeaderboardType();
    
    protected abstract String getMonthlyChannelName();
    
    protected abstract String getAllTimeChannelName();
    
    protected abstract String getStartMessage();
}
