package listeners.commands.preds;

import com.github.twitch4j.chat.events.channel.ChannelMessageEvent;
import com.github.twitch4j.helix.domain.Stream;
import com.netflix.hystrix.exception.HystrixRuntimeException;
import database.DbManager;
import functions.DiscordBotController;
import functions.preds.*;
import listeners.commands.CommandBase;
import util.TwitchApi;
import util.TwitchUserLevel.USER_LEVEL;

import static java.lang.System.out;

public class PredsManagerListener extends CommandBase {
    private static final CommandType COMMAND_TYPE = CommandType.PREFIX_COMMAND;
    private static final USER_LEVEL MIN_USER_LEVEL = USER_LEVEL.BROADCASTER;
    private static final int COOLDOWN = 0;
    private static final CooldownType COOLDOWN_TYPE = CooldownType.COMBINED;
    private static final String PATTERN_PREDS = "!preds";
    private static final String PATTERN_PREDS_CANCEL = "!predscancel";
    
    private static final String GAME_ID_OOT = "11557";
    private static final String GAME_ID_PAPER_MARIO = "18231";
    private static final String GAME_ID_SUNSHINE = "6086";
    private static final String GAME_ID_SMRPG_SWITCH = "1675405846";

    private final DbManager dbManager;
    private final TwitchApi twitchApi;
    private final DiscordBotController discord;
    private final PredsGuessListener predsGuessListener;

    private PredsManagerBase predsManager;

    public PredsManagerListener(
            DbManager dbManager,
            TwitchApi twitchApi,
            DiscordBotController discord,
            PredsGuessListener predsGuessListener
    ) {
        super(COMMAND_TYPE, MIN_USER_LEVEL, COOLDOWN, COOLDOWN_TYPE, PATTERN_PREDS, PATTERN_PREDS_CANCEL);
        this.dbManager = dbManager;
        this.twitchApi = twitchApi;
        this.discord = discord;
        this.predsGuessListener = predsGuessListener;
        predsManager = null;
    }

    @Override
    protected void performCommand(String command, USER_LEVEL userLevel, ChannelMessageEvent messageEvent) {
        switch (command) {
            case PATTERN_PREDS:
                if (predsManager == null || !predsManager.isActive()) {
                    String gameId = "";
                    Stream stream;
                    try {
                        stream = twitchApi.getStreamByUsername(twitchApi.getStreamerUser().getLogin());
                    } catch (HystrixRuntimeException e) {
                        e.printStackTrace();
                        twitchApi.channelMessage("Error retrieving current game");
                        return;
                    }
        
                    if (stream != null) {
                        gameId = stream.getGameId();
                    }
                    switch (gameId) {
                        case GAME_ID_OOT:
                            predsManager = new DampeRacePredsManager(dbManager, discord, twitchApi);
                            break;
                        case GAME_ID_PAPER_MARIO:
                            predsManager = new BadgeShopPredsManager(dbManager, discord, twitchApi);
                            break;
                        case GAME_ID_SUNSHINE:
                            predsManager = new PiantaSixPredsManager(dbManager, discord, twitchApi);
                            break;
                        case GAME_ID_SMRPG_SWITCH:
                            predsManager = new BoosterHillPredsManager(dbManager, discord, twitchApi);
                            break;
                        default:
                            twitchApi.channelMessage("The current game is not compatible with preds.");
                            return;
                    }
                    out.println("Starting the prediction game...");
                    predsGuessListener.start(predsManager);
                    predsManager.startGame();
                } else {
                    if (predsManager.isWaitingForAnswer()) {
                        String[] content = messageEvent.getMessage().split("\\s");
                        if (content.length > 1 && content[1].matches(predsManager.getAnswerRegex())) {
                            out.println("Submitting predictions...");
                            predsManager.submitPredictions(content[1]);
                            predsManager = null;
                        }
                    } else {
                        out.println("Ending the prediction game...");
                        predsGuessListener.stop();
                        predsManager.waitForAnswer();
                    }
                }
                break;
                
            case PATTERN_PREDS_CANCEL:
                if (predsManager != null) {
                    predsManager = null;
                    predsGuessListener.stop();
                    twitchApi.channelAnnouncement("The active preds game has been canceled.");
                } else {
                    twitchApi.channelMessage("There isn't an active preds game to cancel.");
                }
                break;
        }
    }
}
