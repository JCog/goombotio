package Listeners.Commands.Preds;

import Functions.Preds.PapePredsManager;
import Functions.Preds.PredsManagerBase;
import Functions.Preds.SunshinePredsManager;
import Listeners.Commands.CommandBase;
import Util.TwirkInterface;
import Util.TwitchApi;
import Util.TwitchUserLevel;
import com.gikk.twirk.types.twitchMessage.TwitchMessage;
import com.gikk.twirk.types.users.TwitchUser;
import com.github.twitch4j.helix.domain.Game;
import com.github.twitch4j.helix.domain.Stream;
import com.netflix.hystrix.exception.HystrixRuntimeException;

import static java.lang.System.out;

public class PredsManagerListener extends CommandBase {

    private final static String PATTERN = "!preds";
    private static final String GAME_ID_SUNSHINE = "6086";
    private static final String GAME_ID_PAPER_MARIO = "18231";
    
    private final TwirkInterface twirk;
    private final TwitchApi twitchApi;
    private final PredsGuessListener predsGuessListener;
    
    private PredsManagerBase predsManager;

    public PredsManagerListener(TwirkInterface twirk, TwitchApi twitchApi, PredsGuessListener predsGuessListener) {
        super(CommandType.PREFIX_COMMAND);
        this.twirk = twirk;
        this.twitchApi = twitchApi;
        this.predsGuessListener = predsGuessListener;
        predsManager = null;
    }

    @Override
    public String getCommandWords() {
        return PATTERN;
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
        if (predsManager == null || !predsManager.isActive()) {
            String gameId = "";
            Stream stream;
            try {
                stream = twitchApi.getStream();
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
                predsManager = new PapePredsManager(twirk);
            }
            else if (gameId.equals(GAME_ID_SUNSHINE)) {
                predsManager = new SunshinePredsManager(twirk);
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
                }
            }
            else {
                out.println("Ending the prediction game...");
                predsGuessListener.stop();
                predsManager.waitForAnswer();
            }
        }
    }
}
