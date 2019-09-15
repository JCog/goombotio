package Listeners.Commands;

import Util.Database.SpeedySpinLeaderboard;
import com.gikk.twirk.Twirk;
import com.gikk.twirk.enums.USER_TYPE;
import com.gikk.twirk.types.twitchMessage.TwitchMessage;
import com.gikk.twirk.types.users.TwitchUser;

import java.util.ArrayList;

public class SpeedySpinLeaderboardListener extends CommandBase {
    private static final String patternLeaderboard = "!leaderboard";
    private static final String patternBadgeShop = "!badgeshop";
    private static final String patternPoints = "!points";
    private final Twirk twirk;
    private SpeedySpinLeaderboard leaderboard;

    public SpeedySpinLeaderboardListener(Twirk twirk) {
        super(CommandType.PREFIX_COMMAND);
        this.twirk = twirk;
        leaderboard = new SpeedySpinLeaderboard();
    }

    @Override
    protected String getCommandWords() {
        return patternLeaderboard + "|" + patternBadgeShop + "|" + patternPoints;
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
            case patternLeaderboard:
//                ArrayList<Long> topScorers = leaderboard.getTopScorers();
//                ArrayList<Integer> topPoints = new ArrayList<>();
//                ArrayList<String> topNames = new ArrayList<>();
                ArrayList<Long> topMonthlyScorers = leaderboard.getTopMonthlyScorers();
                ArrayList<Integer> topMonthlyPoints = new ArrayList<>();
                ArrayList<String> topMonthlyNames = new ArrayList<>();
//                for (Long topScorer : topScorers) {
//                    topPoints.add(leaderboard.getPoints(topScorer));
//                    topNames.add(leaderboard.getUsername(topScorer));
//                }
                for (Long topMonthlyScorer : topMonthlyScorers) {
                    topMonthlyPoints.add(leaderboard.getMonthlyPoints(topMonthlyScorer));
                    topMonthlyNames.add(leaderboard.getUsername(topMonthlyScorer));
                }

                StringBuilder builder = new StringBuilder();
                builder.append("Monthly Leaderboard: ");
                for (int i = 0; i < topMonthlyNames.size(); i++) {
                    int index = i + 1;
                    String name = topMonthlyNames.get(i);
                    int monthlyPoints = topMonthlyPoints.get(i);

                    builder.append(String.format("%d. %s - %d", index, name, monthlyPoints));

                    if (i + 1 < topMonthlyNames.size()) {
                        builder.append(", ");
                    }
                }

                chatMessage = builder.toString();
                break;

            case patternPoints:
                String username = sender.getDisplayName();
                int points = leaderboard.getMonthlyPoints(sender);
                chatMessage = String.format("@%s you have %d point%s this month.", username, points, points == 1 ? "" : "s");
                break;

            case patternBadgeShop:
                chatMessage = "/me Guess the badge locations in the badge shop! Get 1 point for one badge, 5 for two badges, and 20 if you " +
                        "get all three correct! Use !leaderboard to see the top scores this month and !points to see how many " +
                        "points you have. At the end of every month, JCog will be gifting subs to the top three scorers, so get guessing!";
                break;

            default:
                //should never get here
                chatMessage = "";
        }
        twirk.channelMessage(chatMessage);
    }
}
