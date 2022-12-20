package listeners.commands.preds;

import com.github.twitch4j.chat.events.channel.ChannelMessageEvent;
import com.github.twitch4j.helix.domain.Stream;
import com.github.twitch4j.helix.domain.User;
import com.netflix.hystrix.exception.HystrixRuntimeException;
import database.DbManager;
import functions.DiscordBotController;
import functions.preds.DampeRacePredsManager;
import functions.preds.PredsManagerBase;
import functions.preds.SpeedySpinPredsManager;
import functions.preds.SunshinePredsManager;
import listeners.commands.CommandBase;
import util.TwitchApi;
import util.TwitchUserLevel.USER_LEVEL;

import java.util.concurrent.ScheduledExecutorService;

import static java.lang.System.out;

public class PredsManagerListener extends CommandBase {
    private static final CommandType COMMAND_TYPE = CommandType.PREFIX_COMMAND;
    private static final USER_LEVEL MIN_USER_LEVEL = USER_LEVEL.BROADCASTER;
    private static final int COOLDOWN = 0;
    private static final String PATTERN_PREDS = "!preds";
    private static final String PATTERN_PREDS_CANCEL = "!predscancel";
    
    private static final String GAME_ID_OOT = "11557";
    private static final String GAME_ID_PAPER_MARIO = "18231";
    private static final String GAME_ID_SUNSHINE = "6086";

    private final DbManager dbManager;
    private final TwitchApi twitchApi;
    private final DiscordBotController discord;
    private final PredsGuessListener predsGuessListener;
    private final User streamerUser;

    private PredsManagerBase predsManager;

    public PredsManagerListener(
            ScheduledExecutorService scheduler,
            DbManager dbManager,
            TwitchApi twitchApi,
            DiscordBotController discord,
            PredsGuessListener predsGuessListener,
            User streamerUser
    ) {
        super(scheduler, COMMAND_TYPE, MIN_USER_LEVEL, COOLDOWN, PATTERN_PREDS, PATTERN_PREDS_CANCEL);
        this.dbManager = dbManager;
        this.twitchApi = twitchApi;
        this.discord = discord;
        this.predsGuessListener = predsGuessListener;
        this.streamerUser = streamerUser;
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
                        stream = twitchApi.getStream(streamerUser.getLogin());
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
                            predsManager = new SpeedySpinPredsManager(dbManager, discord, twitchApi, streamerUser);
                            break;
                        case GAME_ID_SUNSHINE:
                            predsManager = new SunshinePredsManager(dbManager, discord, twitchApi);
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
