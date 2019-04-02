package Listeners;

import Functions.SpeedySpinPredictionManager;
import Util.Database.SpeedySpinLeaderboard;
import com.gikk.twirk.Twirk;
import com.gikk.twirk.enums.USER_TYPE;
import com.gikk.twirk.types.twitchMessage.TwitchMessage;
import com.gikk.twirk.types.users.TwitchUser;

import java.util.ArrayList;

public class SpeedySpinLeaderboardListener extends CommandBase {
    private static final String patternLeaderboard = "!leaderboard";
    private static final String patternRules = "!badgeshop";
    private static final String patternPoints = "!points";
    private final Twirk twirk;
    private SpeedySpinLeaderboard leaderboard;

    public SpeedySpinLeaderboardListener(Twirk twirk) {
        super(CommandType.PREFIX_COMMAND);
        this.twirk = twirk;
        leaderboard= new SpeedySpinLeaderboard();
    }

    @Override
    protected String getCommandWords() {
        return patternLeaderboard + "|" + patternRules + "|" + patternPoints;
    }

    @Override
    protected USER_TYPE getMinUserPrivilege() {
        return USER_TYPE.DEFAULT;
    }

    @Override
    protected int getCooldownLength() {
        return 5000;
    }

    @Override
    protected void performCommand(String command, TwitchUser sender, TwitchMessage message) {
        String chatMessage;

        switch (command) {
            case patternLeaderboard:
                ArrayList<Long> topScorers = leaderboard.getTopScorers();
                ArrayList<Integer> topPoints = new ArrayList<>();
                ArrayList<String> topNames = new ArrayList<>();
                for (Long topScorer : topScorers) {
                    topPoints.add(leaderboard.getPoints(topScorer));
                    topNames.add(leaderboard.getUsername(topScorer));
                }
                chatMessage = String.format("Leaderboard: 1. %s - %d, 2. %s - %d, 3. %s - %d",
                        topNames.get(0), topPoints.get(0),
                        topNames.get(1), topPoints.get(1),
                        topNames.get(2), topPoints.get(2));
                break;

            case patternPoints:
                String username = sender.getDisplayName();
                int points = leaderboard.getPoints(sender);
                chatMessage = String.format("@%s you have %d points.", username, points);
                break;

            case patternRules:
                chatMessage = "/me Guess the badge shop! Get 1 point for one badge, 5 for two badges, and 20 if you " +
                        "get all three correct! Use !leaderboard to see the top scores and !points to see how many " +
                        "points you have.";
                break;

            default:
                //should never get here
                chatMessage = "";
        }
        twirk.channelMessage(chatMessage);
    }
}
