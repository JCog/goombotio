package Listeners.Commands;

import Functions.SpeedySpinPredictionManager;
import Functions.SpeedySpinPredictionManager.Badge;
import Util.TwirkInterface;
import Util.TwitchUserLevel;
import com.gikk.twirk.types.twitchMessage.TwitchMessage;
import com.gikk.twirk.types.users.TwitchUser;

import static java.lang.System.out;

public class SpeedySpinGameListener extends CommandBase {

    private final static String PATTERN = "!preds";
    
    private final TwirkInterface twirk;
    private final SpeedySpinPredictionListener guessListener;
    
    private SpeedySpinPredictionManager game;

    public SpeedySpinGameListener(TwirkInterface twirk, SpeedySpinPredictionListener guessListener) {
        super(CommandType.PREFIX_COMMAND);
        this.twirk = twirk;
        this.guessListener = guessListener;
        game = new SpeedySpinPredictionManager(twirk);
    }

    @Override
    protected String getCommandWords() {
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
        if (game.isEnabled()) {
            if (game.isWaitingForAnswer()) {
                String[] content = message.getContent().split("\\s");
                if (content.length > 1 && content[1].matches("[1-4]{3}")) {
                    out.println("Submitting predictions...");
                    game.submitPredictions(
                            Badge.values()[Character.getNumericValue(content[1].charAt(0)) - 1],
                            Badge.values()[Character.getNumericValue(content[1].charAt(1)) - 1],
                            Badge.values()[Character.getNumericValue(content[1].charAt(2)) - 1]
                    );
                }
            }
            else {
                out.println("Ending the prediction game...");
                guessListener.stop();
                game.stop();
            }
        }
        else {
            game = new SpeedySpinPredictionManager(twirk);
            guessListener.start(game);
            out.println("Starting the prediction game...");
            game.start();
        }
    }
}
