package listeners.commands.preds;

import com.gikk.twirk.enums.USER_TYPE;
import com.gikk.twirk.types.twitchMessage.TwitchMessage;
import com.gikk.twirk.types.users.TwitchUser;
import com.github.twitch4j.helix.domain.Stream;
import com.jcog.utils.TwitchApi;
import com.jcog.utils.TwitchUserLevel;
import com.jcog.utils.database.DbManager;
import com.jcog.utils.database.preds.PredsLeaderboardDb;
import com.netflix.hystrix.exception.HystrixRuntimeException;
import listeners.commands.CommandBase;
import util.TwirkInterface;

import java.util.ArrayList;
import java.util.concurrent.ScheduledExecutorService;

public class LeaderboardListener extends CommandBase {
    private static final String PREDS_MESSAGE_PAPE = "Guess the badge locations in the badge shop! Get 1 point for one badge (or if you have them all but in the wrong locations), 5 for two badges, and 20 if you get all three correct! Use !leaderboard to see the top scores this month and !points to see how many points you have. If you get all three and aren't subscribed to the channel, JCog will gift you a sub, and at the end of every month, the top five scorers will be given a VIP badge for the next month, so get guessing!";
    private static final String PREDS_MESSAGE_SMS = "Guess what the timer will be at the end of Pianta 6! You get 1 point if you're within ten seconds, 5 points if you're within five seconds, 15 points if you're within 1 second, and if you get it exactly right you'll get 50 points and a free gift sub! If nobody is exactly right, whoever's closest will get an additional 10 point bonus, and at the end of every month, the top five scorers will be given a VIP badge for the next month, so get guessing!";
    private static final String PREDS_MESSAGE_DEFAULT = "Either the stream isn't live or the current game is not associated with preds";

    private static final String GAME_ID_SUNSHINE = "6086";
    private static final String GAME_ID_PAPER_MARIO = "18231";
    private static final String PATTERN_LEADERBOARD = "!leaderboard";
    private static final String PATTERN_PREDS = "!preds";
    private static final String PATTERN_POINTS = "!points";
    private static final String PATTERN_LEADERBOARD_ALL = "!leaderboardall";
    private static final String PATTERN_POINTS_ALL = "!pointsall";

    private final TwirkInterface twirk;
    private final DbManager dbManager;
    private final TwitchApi twitchApi;

    private PredsLeaderboardDb leaderboard;

    public LeaderboardListener(ScheduledExecutorService scheduler,
                               TwirkInterface twirk,
                               DbManager dbManager,
                               TwitchApi twitchApi) {
        super(CommandType.PREFIX_COMMAND, scheduler);
        this.twirk = twirk;
        this.dbManager = dbManager;
        this.twitchApi = twitchApi;
        updateLeaderboardType();
    }

    @Override
    public String getCommandWords() {
        return String.join("|",
                           PATTERN_LEADERBOARD,
                           PATTERN_PREDS,
                           PATTERN_POINTS,
                           PATTERN_LEADERBOARD_ALL,
                           PATTERN_POINTS_ALL
        );
    }

    @Override
    protected TwitchUserLevel.USER_LEVEL getMinUserPrivilege() {
        return TwitchUserLevel.USER_LEVEL.DEFAULT;
    }

    @Override
    protected int getCooldownLength() {
        return 100;
    }

    @Override
    protected void performCommand(String command, TwitchUser sender, TwitchMessage message) {
        String chatMessage = "";

        updateLeaderboardType();
        switch (command) {
            case PATTERN_LEADERBOARD:
                chatMessage = buildMonthlyLeaderboardString();
                break;

            case PATTERN_POINTS:
                chatMessage = buildMonthlyPointsString(sender);
                break;

            case PATTERN_PREDS:
                if (sender.getUserType() != USER_TYPE.OWNER) {
                    switch (getGameId()) {
                        case GAME_ID_PAPER_MARIO:
                            chatMessage = PREDS_MESSAGE_PAPE;
                            break;
                        case GAME_ID_SUNSHINE:
                            chatMessage = PREDS_MESSAGE_SMS;
                            break;
                        default:
                            chatMessage = PREDS_MESSAGE_DEFAULT;
                    }
                }
                break;

            case PATTERN_LEADERBOARD_ALL:
                chatMessage = buildAllTimeLeaderboardString();
                break;

            case PATTERN_POINTS_ALL:
                chatMessage = buildPointsString(sender);

                break;
        }
        twirk.channelCommand(chatMessage);
    }

    private void updateLeaderboardType() {
        switch (getGameId()) {
            case GAME_ID_PAPER_MARIO:
                leaderboard = dbManager.getSpeedySpinLeaderboardDb();
                break;
            case GAME_ID_SUNSHINE:
            default:
                leaderboard = dbManager.getSunshineTimerLeaderboardDb();
                break;
        }
    }

    private String getGameId() {
        Stream stream;
        try {
            stream = twitchApi.getStream();
        }
        catch (HystrixRuntimeException e) {
            e.printStackTrace();
            System.out.println("Error retrieving stream data");
            return "";
        }
        if (stream != null) {
            return stream.getGameId();
        }
        return "";
    }

    private String buildMonthlyPointsString(TwitchUser user) {
        String username = user.getDisplayName();
        int points = leaderboard.getMonthlyPoints(user);
        return String.format("@%s you have %d point%s this month.", username, points, points == 1 ? "" : "s");
    }

    private String buildPointsString(TwitchUser user) {
        String username = user.getDisplayName();
        int points = leaderboard.getPoints(user);
        return String.format("@%s you have %d total point%s.", username, points, points == 1 ? "" : "s");
    }

    private String buildMonthlyLeaderboardString() {
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

    private String buildAllTimeLeaderboardString() {
        ArrayList<Long> topScorers = leaderboard.getTopThreeScorers();
        ArrayList<Integer> topPoints = new ArrayList<>();
        ArrayList<String> topNames = new ArrayList<>();
        for (Long topScorer : topScorers) {
            topPoints.add(leaderboard.getPoints(topScorer));
            topNames.add(leaderboard.getUsername(topScorer));
        }

        int prevPoints = -1;
        int prevRank = -1;
        ArrayList<String> leaderboardStrings = new ArrayList<>();
        for (int i = 0; i < topNames.size(); i++) {
            if (topPoints.get(i) != prevPoints) {
                prevRank = i + 1;
                if (prevRank > 5) {
                    break;
                }
            }
            prevPoints = topPoints.get(i);
            String name = topNames.get(i);

            leaderboardStrings.add(String.format("%d. %s - %d", prevRank, name, prevPoints));
        }

        return "All-Time Leaderboard: " + String.join(", ", leaderboardStrings);
    }
}
