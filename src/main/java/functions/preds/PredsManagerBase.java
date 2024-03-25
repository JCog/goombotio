package functions.preds;

import database.misc.VipDb;
import database.misc.VipRaffleDb;
import database.preds.PredsLeaderboardDbBase.PredsItem;
import functions.DiscordBotController;
import util.CommonUtils;
import util.TwitchApi;

import java.util.List;
import java.util.stream.Collectors;

public abstract class PredsManagerBase {
    private static final String STOP_MESSAGE = "Predictions are up! Let's see how everyone did...";
    private static final int DISCORD_MAX_CHARS = 2000;
    
    final DiscordBotController discord;
    final TwitchApi twitchApi;
    final VipRaffleDb vipRaffleDb;
    final VipDb vipDb;
    final String startMessage;
    final String answerRegex;

    boolean isEnabled;
    boolean waitingForAnswer;

    PredsManagerBase(CommonUtils commonUtils, String startMessage, String answerRegex) {
        twitchApi = commonUtils.getTwitchApi();
        vipRaffleDb = commonUtils.getDbManager().getVipRaffleDb();
        vipDb = commonUtils.getDbManager().getVipDb();
        discord = commonUtils.getDiscordBotController();
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
    
    public abstract void submitPredictions(String answer);
    
    public abstract void makePredictionIfValid(String userId, String displayName, String message);
    
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    
    public void updateDiscordLeaderboardWins(String discordChannel, String messageTitle, List<PredsItem> sortedItems) {
        List<String> names = sortedItems.stream().map(PredsItem::getDisplayName).collect(Collectors.toList());
        List<Integer> winCounts = sortedItems.stream().map(PredsItem::getWins).collect(Collectors.toList());
        updateDiscordLeaderboard(discordChannel, messageTitle, names, winCounts);
    }
    
    public void updateDiscordLeaderboardPoints(String discordChannel, String messageTitle, List<PredsItem> sortedItems) {
        List<String> names = sortedItems.stream().map(PredsItem::getDisplayName).collect(Collectors.toList());
        List<Integer> pointCounts = sortedItems.stream().map(PredsItem::getPoints).collect(Collectors.toList());
        updateDiscordLeaderboard(discordChannel, messageTitle, names, pointCounts);
    }
    
    private void updateDiscordLeaderboard(
            String discordChannel, String messageTitle, List<String> names, List<Integer> values
    ) {
        StringBuilder message = new StringBuilder();
        message.append(messageTitle).append("\n```");
    
        // add entries until discord character limit is reached
        int prevValue = -1;
        int prevRank = -1;
        for (int i = 0; i < values.size(); i++) {
            if (values.get(i) != prevValue) {
                prevRank = i + 1;
            }
            prevValue = values.get(i);
            String nextString = String.format("%d. %s - %d\n", prevRank, names.get(i), values.get(i));
            if (message.length() + nextString.length() > DISCORD_MAX_CHARS - 3) {
                break;
            } else {
                message.append(nextString);
            }
        }
        message.append("```");
    
        if (discord.hasRecentMessageContents(discordChannel)) {
            discord.editMostRecentMessage(discordChannel, message.toString());
        } else {
            discord.sendMessage(discordChannel, message.toString());
        }
    }
}
