package Listeners.Commands.Preds;

import Functions.Preds.PapePredsManager;
import Functions.Preds.PapePredsManager.Badge;
import Listeners.Commands.CommandBase;
import Util.TwirkInterface;
import Util.TwitchUserLevel;
import com.gikk.twirk.types.twitchMessage.TwitchMessage;
import com.gikk.twirk.types.users.TwitchUser;

import static java.lang.System.out;

public class PredsManagerListener extends CommandBase {

    private final static String PATTERN = "!preds";
    
    private final TwirkInterface twirk;
    private final PapeGuessListener papeGuessListener;
    
    private PapePredsManager papePredsManager;

    public PredsManagerListener(TwirkInterface twirk, PapeGuessListener papeGuessListener) {
        super(CommandType.PREFIX_COMMAND);
        this.twirk = twirk;
        this.papeGuessListener = papeGuessListener;
        papePredsManager = new PapePredsManager(twirk);
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
        if (papePredsManager.isEnabled()) {
            if (papePredsManager.isWaitingForAnswer()) {
                String[] content = message.getContent().split("\\s");
                if (content.length > 1 && content[1].matches("[1-4]{3}")) {
                    out.println("Submitting predictions...");
                    papePredsManager.submitPredictions(
                            Badge.values()[Character.getNumericValue(content[1].charAt(0)) - 1],
                            Badge.values()[Character.getNumericValue(content[1].charAt(1)) - 1],
                            Badge.values()[Character.getNumericValue(content[1].charAt(2)) - 1]
                    );
                }
            }
            else {
                out.println("Ending the prediction game...");
                papeGuessListener.stop();
                papePredsManager.stop();
            }
        }
        else {
            papePredsManager = new PapePredsManager(twirk);
            papeGuessListener.start(papePredsManager);
            out.println("Starting the prediction game...");
            papePredsManager.start();
        }
    }
}
