package Listeners.Commands;

import Util.Database.SpeedySpinLeaderboard;
import Util.TwirkInterface;
import com.gikk.twirk.enums.USER_TYPE;
import com.gikk.twirk.types.twitchMessage.TwitchMessage;
import com.gikk.twirk.types.users.TwitchUser;

import java.util.ArrayList;

public class SpeedySpinLeaderboardListener extends CommandBase {
    
    private static final String PATTERN_LEADERBOARD = "!leaderboard";
    private static final String PATTERN_BADGE_SHOP = "!badgeshop";
    private static final String PATTERN_POINTS = "!points";
    private static final String PATTERN_LEADERBOARD_ALL = "!leaderboardall";
    private static final String PATTERN_POINTS_ALL = "!pointsall";
    
    private final TwirkInterface twirk;
    
    private SpeedySpinLeaderboard leaderboard;

    public SpeedySpinLeaderboardListener(TwirkInterface twirk) {
        super(CommandType.PREFIX_COMMAND);
        this.twirk = twirk;
        leaderboard = SpeedySpinLeaderboard.getInstance();
    }

    @Override
    protected String getCommandWords() {
        return String.join("|",
                PATTERN_LEADERBOARD,
                PATTERN_BADGE_SHOP,
                PATTERN_POINTS,
                PATTERN_LEADERBOARD_ALL,
                PATTERN_POINTS_ALL);
    }

    @Override
    protected USER_TYPE getMinUserPrivilege() {
        return USER_TYPE.DEFAULT;
    }

    @Override
    protected int getCooldownLength() {
        return 100;
    }

    @Override
    protected void performCommand(String command, TwitchUser sender, TwitchMessage message) {
        String chatMessage;

        switch (command) {
            case PATTERN_LEADERBOARD:
                chatMessage = buildMonthlyLeaderboardString();
                break;

            case PATTERN_POINTS:
                chatMessage = buildMonthlyPointsString(sender);
                break;

            case PATTERN_BADGE_SHOP:
                chatMessage = "/me Guess the badge locations in the badge shop! Get 1 point for one badge (or if you " +
                        "have them all but in the wrong locations), 5 for two badges, and 20 if you get all three " +
                        "correct! Use !leaderboard to see the top scores this month and !points to see how many " +
                        "points you have. If you get all three and aren't subscribed to the channel, JCog will gift " +
                        "you a sub, and at the end of every month, the top three scorers will be given a VIP badge " +
                        "for the next month, so get guessing!";
                break;
                
            case PATTERN_LEADERBOARD_ALL:
                chatMessage = buildAllTimeLeaderboardString();
                break;
                
            case PATTERN_POINTS_ALL:
                chatMessage = buildPointsString(sender);
                
                break;

            default:
                //should never get here
                chatMessage = "";
        }
        twirk.channelMessage(chatMessage);
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
