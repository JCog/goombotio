package listeners.commands.preds;

import com.gikk.twirk.types.twitchMessage.TwitchMessage;
import com.gikk.twirk.types.users.TwitchUser;
import com.github.twitch4j.helix.domain.Stream;
import com.github.twitch4j.helix.domain.User;
import com.netflix.hystrix.exception.HystrixRuntimeException;
import database.DbManager;
import functions.DiscordBotController;
import functions.preds.PapePredsManager;
import functions.preds.PredsManagerBase;
import functions.preds.SunshinePredsManager;
import listeners.commands.CommandBase;
import util.TwirkInterface;
import util.TwitchApi;
import util.TwitchUserLevel;

import java.util.concurrent.ScheduledExecutorService;

import static java.lang.System.out;

public class PredsManagerListener extends CommandBase {

    private final static String PATTERN_PREDS = "!preds";
    private static final String PATTERN_PREDS_CANCEL = "!predscancel";
    private static final String GAME_ID_SUNSHINE = "6086";
    private static final String GAME_ID_PAPER_MARIO = "18231";

    private final TwirkInterface twirk;
    private final DbManager dbManager;
    private final TwitchApi twitchApi;
    private final DiscordBotController discord;
    private final PredsGuessListener predsGuessListener;
    private final User streamerUser;

    private PredsManagerBase predsManager;

    public PredsManagerListener(
            ScheduledExecutorService scheduler,
            TwirkInterface twirk,
            DbManager dbManager,
            TwitchApi twitchApi,
            DiscordBotController discord,
            PredsGuessListener predsGuessListener,
            User streamerUser
    ) {
        super(CommandType.PREFIX_COMMAND, scheduler);
        this.twirk = twirk;
        this.dbManager = dbManager;
        this.twitchApi = twitchApi;
        this.discord = discord;
        this.predsGuessListener = predsGuessListener;
        this.streamerUser = streamerUser;
        predsManager = null;
    }

    @Override
    public String getCommandWords() {
        return String.join("|", PATTERN_PREDS, PATTERN_PREDS_CANCEL);
    }

    @Override
    protected TwitchUserLevel.USER_LEVEL getMinUserPrivilege() {
        return TwitchUserLevel.USER_LEVEL.BROADCASTER;
    }

    @Override
    protected int getCooldownLength() {
        return 0;
    }

    @Override
    protected void performCommand(String command, TwitchUser sender, TwitchMessage message) {
        switch (command) {
            case PATTERN_PREDS:
                if (predsManager == null || !predsManager.isActive()) {
                    String gameId = "";
                    Stream stream;
                    try {
                        stream = twitchApi.getStream(streamerUser.getLogin());
                    }
                    catch (HystrixRuntimeException e) {
                        e.printStackTrace();
                        twirk.channelMessage("Error retrieving current game");
                        return;
                    }
        
                    if (stream != null) {
                        gameId = stream.getGameId();
                    }
                    if (gameId.equals(GAME_ID_PAPER_MARIO)) {
                        predsManager = new PapePredsManager(twirk, dbManager, discord, twitchApi, streamerUser);
                    }
                    else if (gameId.equals(GAME_ID_SUNSHINE)) {
                        predsManager = new SunshinePredsManager(twirk, dbManager, discord, twitchApi, streamerUser);
                    }
                    else {
                        twirk.channelMessage("The current game is not compatible with preds.");
                        return;
                    }
                    out.println("Starting the prediction game...");
                    predsGuessListener.start(predsManager);
                    predsManager.startGame();
                }
                else {
                    if (predsManager.isWaitingForAnswer()) {
                        String[] content = message.getContent().split("\\s");
                        if (content.length > 1 && content[1].matches(predsManager.getAnswerRegex())) {
                            out.println("Submitting predictions...");
                            predsManager.submitPredictions(content[1]);
                            predsManager = null;
                        }
                    }
                    else {
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
                    twirk.channelCommand("Active preds game has been canceled.");
                }
                else {
                    twirk.channelCommand("There isn't an active preds game to cancel.");
                }
                break;
        }
    }
}
