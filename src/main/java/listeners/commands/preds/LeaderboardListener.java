package listeners.commands.preds;

import com.github.twitch4j.chat.events.channel.ChannelMessageEvent;
import com.github.twitch4j.common.events.domain.EventUser;
import com.github.twitch4j.helix.domain.Stream;
import com.github.twitch4j.helix.domain.User;
import com.netflix.hystrix.exception.HystrixRuntimeException;
import database.DbManager;
import database.preds.PredsLeaderboardDb;
import functions.preds.PredsManagerBase;
import listeners.commands.CommandBase;
import util.TwitchApi;
import util.TwitchUserLevel.USER_LEVEL;

import java.util.ArrayList;
import java.util.concurrent.ScheduledExecutorService;

public class LeaderboardListener extends CommandBase {
    private static final CommandType COMMAND_TYPE = CommandType.PREFIX_COMMAND;
    private static final USER_LEVEL MIN_USER_LEVEL = USER_LEVEL.DEFAULT;
    private static final int COOLDOWN = 100;
    private static final String PATTERN_LEADERBOARD = "!leaderboard";
    private static final String PATTERN_PREDS = "!preds";
    private static final String PATTERN_POINTS = "!points";
    private static final String PATTERN_LEADERBOARD_ALL = "!leaderboardall";
    private static final String PATTERN_POINTS_ALL = "!pointsall";
    
    private static final String PREDS_MESSAGE_PAPE = "Guess the badge locations in the badge shop! Get 1 point for one badge (or if you have them all but in the wrong locations), 5 for two badges, and 20 if you get all three correct! Use !leaderboard to see the top scores this month and !points to see how many points you have. If you get all three and aren't subscribed to the channel, JCog will gift you a sub, and at the end of every month, the top five scorers will be given a VIP badge for the next month, so get guessing!";
    private static final String PREDS_MESSAGE_SMS = "Guess what the timer will be at the end of Pianta 6! You get 1 point if you're within ten seconds, 5 points if you're within five seconds, 15 points if you're within 1 second, and if you get it exactly right you'll get 50 points and a free gift sub! If nobody is exactly right, whoever's closest will get an additional 10 point bonus, and at the end of every month, the top five scorers will be given a VIP badge for the next month, so get guessing!";
    private static final String PREDS_MESSAGE_DEFAULT = "Either the stream isn't live or the current game is not associated with preds";

    private static final String GAME_ID_SUNSHINE = "6086";
    private static final String GAME_ID_PAPER_MARIO = "18231";

    private final DbManager dbManager;
    private final TwitchApi twitchApi;
    private final User streamerUser;

    private PredsLeaderboardDb leaderboard;

    public LeaderboardListener(
            ScheduledExecutorService scheduler,
            DbManager dbManager,
            TwitchApi twitchApi,
            User streamerUser
    ) {
        super(
                scheduler,
                COMMAND_TYPE,
                MIN_USER_LEVEL,
                COOLDOWN,
                PATTERN_LEADERBOARD,
                PATTERN_PREDS,
                PATTERN_POINTS,
                PATTERN_LEADERBOARD_ALL,
                PATTERN_POINTS_ALL
        );
        this.dbManager = dbManager;
        this.twitchApi = twitchApi;
        this.streamerUser = streamerUser;
        updateLeaderboardType();
    }

    @Override
    protected void performCommand(String command, USER_LEVEL userLevel, ChannelMessageEvent messageEvent) {
        String chatMessage = "";

        updateLeaderboardType();
        if (leaderboard == null) {
            chatMessage = PREDS_MESSAGE_DEFAULT;
        } else {
            switch (command) {
                case PATTERN_LEADERBOARD:
                    chatMessage = PredsManagerBase.buildMonthlyLeaderboardString(
                            leaderboard,
                            dbManager.getPermanentVipsDb(),
                            twitchApi,
                            streamerUser
                    );
                    break;
        
                case PATTERN_POINTS:
                    chatMessage = buildMonthlyPointsString(messageEvent.getUser());
                    break;
        
                case PATTERN_PREDS:
                    if (userLevel != USER_LEVEL.BROADCASTER) {
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
                    chatMessage = buildPointsString(messageEvent.getUser());
                    break;
            }
        }
        twitchApi.channelCommand(chatMessage);
    }

    private void updateLeaderboardType() {
        switch (getGameId()) {
            case GAME_ID_PAPER_MARIO:
                leaderboard = dbManager.getSpeedySpinLeaderboardDb();
                break;
            case GAME_ID_SUNSHINE:
                leaderboard = dbManager.getSunshineTimerLeaderboardDb();
                break;
            default:
                leaderboard = null;
        }
    }

    private String getGameId() {
        Stream stream;
        try {
            stream = twitchApi.getStream(streamerUser.getLogin());
        } catch (HystrixRuntimeException e) {
            e.printStackTrace();
            System.out.println("Error retrieving stream data");
            return "";
        }
        if (stream != null) {
            return stream.getGameId();
        }
        return "";
    }

    private String buildMonthlyPointsString(EventUser user) {
        String username = user.getName();
        int points = leaderboard.getMonthlyPoints(user);
        return String.format("@%s you have %d point%s this month.", username, points, points == 1 ? "" : "s");
    }

    private String buildPointsString(EventUser user) {
        String username = user.getName();
        int points = leaderboard.getPoints(user);
        return String.format("@%s you have %d total point%s.", username, points, points == 1 ? "" : "s");
    }

    private String buildAllTimeLeaderboardString() {
        ArrayList<Long> topScorers = leaderboard.getTopScorers(5);
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
