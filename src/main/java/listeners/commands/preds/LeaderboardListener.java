package listeners.commands.preds;

import com.github.twitch4j.chat.events.channel.ChannelMessageEvent;
import com.github.twitch4j.helix.domain.Stream;
import com.netflix.hystrix.exception.HystrixRuntimeException;
import database.DbManager;
import database.preds.PredsLeaderboardDbBase;
import listeners.commands.CommandBase;
import util.CommonUtils;
import util.TwitchApi;
import util.TwitchUserLevel.USER_LEVEL;

import java.util.ArrayList;
import java.util.List;

public class LeaderboardListener extends CommandBase {
    private static final CommandType COMMAND_TYPE = CommandType.PREFIX_COMMAND;
    private static final USER_LEVEL MIN_USER_LEVEL = USER_LEVEL.DEFAULT;
    private static final int COOLDOWN = 2;
    private static final CooldownType COOLDOWN_TYPE = CooldownType.PER_USER;
    private static final String PATTERN_LEADERBOARD = "!leaderboard";
    private static final String PATTERN_PREDS = "!preds";
    
    private static final String PREDS_MESSAGE_OOT =
            "Guess what the timer will say at the end of the Dampe race to win raffle entries for next month's VIP " +
            "raffle! Get 2 for being two seconds off, 5 for being one second off, or 15 for guessing correctly! " +
            "You'll always get at least one entry just for participating, so get guessing!";
    private static final String PREDS_MESSAGE_PAPE =
            "Guess the badge locations in the badge shop to win raffle entries for next month's VIP raffle! Get 2 " +
            "for having one badge correct (or all correct but in the wrong locations), 5 for two badges, and 20 if " +
            "you guess all three correctly! You'll always get at least one entry just for participating, so get " +
            "guessing!";
    private static final String PREDS_MESSAGE_SMS = //TODO: update for the VIP raffle
            "Guess what the timer will be at the end of Pianta 6! You get 1 point if you're within ten seconds, 5 " +
            "points if you're within five seconds, 15 points if you're within 1 second, and if you get it exactly " +
            "right you'll get 50 points and a free gift sub! If nobody is exactly right, whoever's closest will get " +
            "an additional 10 point bonus, and at the end of every month, the top five scorers will be given a VIP " +
            "badge for the next month, so get guessing!";
    private static final String PREDS_MESSASGE_SMRPG_SWITCH =
            "Guess how many Flowers JCog will get during Booster Hill to win raffle entries for next month's VIP " +
            "raffle! Get 2 for being two flowers off, 5 for being one off, or 20 for guessing correctly! You'll " +
            "always get at least one entry just for participating, so get guessing!";
    private static final String PREDS_MESSAGE_DEFAULT =
            "Either the stream isn't live or the current game does not have a preds leaderboard.";
    
    private static final String GAME_ID_OOT = "11557";
    private static final String GAME_ID_PAPER_MARIO = "18231";
    private static final String GAME_ID_SUNSHINE = "6086";
    private static final String GAME_ID_SMRPG_SWITCH = "1675405846";

    private final DbManager dbManager;
    private final TwitchApi twitchApi;

    private PredsLeaderboardDbBase leaderboard;

    public LeaderboardListener(CommonUtils commonUtils) {
        super(
                COMMAND_TYPE,
                MIN_USER_LEVEL,
                COOLDOWN,
                COOLDOWN_TYPE,
                PATTERN_LEADERBOARD,
                PATTERN_PREDS
        );
        dbManager = commonUtils.dbManager();
        twitchApi = commonUtils.twitchApi();
        updateLeaderboardType();
    }

    @Override
    protected void performCommand(String command, USER_LEVEL userLevel, ChannelMessageEvent messageEvent) {
        updateLeaderboardType();
        if (leaderboard == null) {
            twitchApi.channelMessage(PREDS_MESSAGE_DEFAULT);
            return;
        }

        String chatMessage = switch (command) {
            case PATTERN_LEADERBOARD -> buildLeaderboardString();
            case PATTERN_PREDS -> {
                if (userLevel == USER_LEVEL.BROADCASTER) {
                    yield "";
                }
                yield switch (getGameId()) {
                    case GAME_ID_OOT -> PREDS_MESSAGE_OOT;
                    case GAME_ID_PAPER_MARIO -> PREDS_MESSAGE_PAPE;
                    case GAME_ID_SUNSHINE -> PREDS_MESSAGE_SMS;
                    case GAME_ID_SMRPG_SWITCH -> PREDS_MESSASGE_SMRPG_SWITCH;
                    default -> PREDS_MESSAGE_DEFAULT;
                };
            }
            default -> "";
        };
        twitchApi.channelMessage(chatMessage);
    }

    private void updateLeaderboardType() {
        switch (getGameId()) {
            case GAME_ID_OOT -> leaderboard = dbManager.getDampeRaceLeaderboardDb();
            case GAME_ID_PAPER_MARIO -> leaderboard = dbManager.getBadgeShopLeaderboardDb();
            case GAME_ID_SUNSHINE -> leaderboard = dbManager.getPiantaSixLeaderboardDb();
            case GAME_ID_SMRPG_SWITCH -> leaderboard = dbManager.getBoosterHillLeaderboardDb();
            default -> leaderboard = null;
        }
    }

    private String getGameId() {
        Stream stream;
        try {
            stream = twitchApi.getStreamByUserId(twitchApi.getStreamerUser().getId());
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

    private String buildLeaderboardString() {
        // retrieve 5 extra winners in case of ties
        List<String> topWinners = leaderboard.getTopWinners(10);
        List<Integer> topWins = new ArrayList<>();
        List<String> topNames = new ArrayList<>();
        for (String winner : topWinners) {
            topWins.add(leaderboard.getWins(winner));
            topNames.add(leaderboard.getUsername(winner));
        }

        int prevWins = -1;
        int prevRank = -1;
        List<String> leaderboardStrings = new ArrayList<>();
        for (int i = 0; i < topNames.size(); i++) {
            // show duplicate rank if people are tied
            if (topWins.get(i) != prevWins) {
                prevRank = i + 1;
                // show at least 5 names, but none past 5th place (can have 2 in 5th, 3 in 4th, etc.)
                if (prevRank > 5) {
                    break;
                }
            }
            prevWins = topWins.get(i);
            String name = topNames.get(i);

            leaderboardStrings.add(String.format("%d. %s - %d", prevRank, name, prevWins));
        }

        return "!preds Leaderboard: " + String.join(", ", leaderboardStrings);
    }
}
