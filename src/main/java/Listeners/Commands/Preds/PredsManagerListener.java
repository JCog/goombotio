package Listeners.Commands.Preds;

import Functions.Preds.PapePredsManager;
import Functions.Preds.PredsManagerBase;
import Functions.Preds.SunshinePredsManager;
import Functions.StreamInfo;
import Listeners.Commands.CommandBase;
import Util.TwirkInterface;
import Util.TwitchUserLevel;
import com.gikk.twirk.types.twitchMessage.TwitchMessage;
import com.gikk.twirk.types.users.TwitchUser;

import static java.lang.System.out;

public class PredsManagerListener extends CommandBase {

    private final static String PATTERN = "!preds";
    private static final String GAME_SUNSHINE = "Super Mario Sunshine";
    private static final String GAME_PAPER_MARIO = "Paper Mario";
    
    private final TwirkInterface twirk;
    private final StreamInfo streamInfo;
    private final PredsGuessListener predsGuessListener;
    
    private PredsManagerBase predsManager;

    public PredsManagerListener(TwirkInterface twirk, StreamInfo streamInfo, PredsGuessListener predsGuessListener) {
        super(CommandType.PREFIX_COMMAND);
        this.twirk = twirk;
        this.streamInfo = streamInfo;
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
            if (streamInfo.getGame().equals(GAME_PAPER_MARIO)) {
                predsManager = new PapePredsManager(twirk);
            }
            //else if (streamInfo.getGame().equals(GAME_SUNSHINE)) {
            else {
                predsManager = new SunshinePredsManager(twirk);
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
