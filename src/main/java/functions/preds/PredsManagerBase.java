package functions.preds;

import com.github.twitch4j.common.events.domain.EventUser;
import com.github.twitch4j.helix.domain.Moderator;
import com.github.twitch4j.helix.domain.User;
import database.DbManager;
import database.preds.PredsLeaderboardDb;
import functions.DiscordBotController;
import util.TwitchApi;

import java.io.File;
import java.text.SimpleDateFormat;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public abstract class PredsManagerBase {
    private static final int DISCORD_MAX_CHARS = 2000;
    private static final String STOP_MESSAGE = "/me Predictions are up! Let's see how everyone did...";
    private static final String VIP_FILENAME = "vips.txt";

    protected final String START_MESSAGE = getStartMessage();


    protected final PredsLeaderboardDb leaderboard;

    private final DiscordBotController discord;
    
    protected final TwitchApi twitchApi;
    protected final DbManager dbManager;

    protected boolean enabled;
    protected boolean waitingForAnswer;
    
    private static HashSet<String> permanentVips = null;

    protected PredsManagerBase(TwitchApi twitchApi, DbManager dbManager, DiscordBotController discord) {
        this.twitchApi = twitchApi;
        this.dbManager = dbManager;
        this.discord = discord;
        leaderboard = getLeaderboardType();
    }

    /**
     * Returns true if there is an active !preds game
     *
     * @return enabled state
     */
    public boolean isActive() {
        return enabled;
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
        enabled = true;
        twitchApi.channelCommand(START_MESSAGE);
    }

    /**
     * Sets the game to a state where it's waiting for the correct answer and sends a message to the chat to let them
     * know to stop submitting predictions.
     */
    public void waitForAnswer() {
        waitingForAnswer = true;
        twitchApi.channelCommand(STOP_MESSAGE);
    }

    public void endGame() {
        waitingForAnswer = false;
        enabled = false;
    }

    public abstract void submitPredictions(String answer);

    public abstract String getAnswerRegex();

    public abstract void makePredictionIfValid(EventUser user, String message);
    
    protected static void updateDiscordMonthlyPoints(PredsLeaderboardDb leaderboard, DiscordBotController discord, String channel) {
        Thread thread = new Thread(() -> {
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
            if (discord.hasRecentMessageContents(channel)) {
                String dateString = discord.getMostRecentMessageContents(channel).split("\n", 2)[0];
                YearMonth now = YearMonth.now();
                YearMonth messageDate = YearMonth.parse(dateString, DateTimeFormatter.ofPattern("MMMM yyyy"));
                if (now.getMonth() == messageDate.getMonth() && now.getYear() == messageDate.getYear()) {
                    discord.editMostRecentMessage(channel, message.toString());
                    System.out.printf("%s - current month edited.\n", channel);
                    return;
                }
            }
            discord.sendMessage(channel, message.toString());
            System.out.printf("%s - new month added.\n", channel);
        });
        thread.start();
    }
    
    protected static void updateDiscordAllTimePoints(PredsLeaderboardDb leaderboard, DiscordBotController discord, String channel) {
        Thread thread = new Thread(() -> {
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
            
            if (discord.hasRecentMessageContents(channel)) {
                discord.editMostRecentMessage(channel, message.toString());
            }
            else {
                discord.sendMessage(channel, message.toString());
            }
            System.out.printf("%s - updated.\n", channel);
        });
        thread.start();
    }

    public static String buildMonthlyLeaderboardString(PredsLeaderboardDb leaderboard, TwitchApi twitchApi, User streamer) {
        ArrayList<Long> topMonthlyScorers = leaderboard.getTopMonthlyScorers();
        ArrayList<Integer> topMonthlyPoints = new ArrayList<>();
        ArrayList<String> topMonthlyNames = new ArrayList<>();
        List<String> mods = twitchApi.getMods(streamer.getId()).stream().map(Moderator::getUserLogin).collect(Collectors.toList());
        HashSet<String> permanentVips = getPermanentVips();
    
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
    
    private static HashSet<String> getPermanentVips() {
        if (permanentVips == null) {
    
            HashSet<String> tempList = new HashSet<>();
            try {
                File file = new File(VIP_FILENAME);
                Scanner sc = new Scanner(file);
                while (sc.hasNextLine()) {
                    tempList.add(sc.nextLine());
                }
                sc.close();
                permanentVips = tempList;
            } catch (Exception e) {
                e.printStackTrace();
                return new HashSet<>();
            }
        }
        
        return permanentVips;
    }

    protected abstract PredsLeaderboardDb getLeaderboardType();

    protected abstract String getMonthlyChannelName();

    protected abstract String getAllTimeChannelName();

    protected abstract String getStartMessage();
}
