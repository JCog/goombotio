package Listeners;

import Functions.SpeedySpinPredictionManager;
import Functions.SpeedySpinPredictionManager.Badge;
import com.gikk.twirk.Twirk;
import com.gikk.twirk.enums.USER_TYPE;
import com.gikk.twirk.types.twitchMessage.TwitchMessage;
import com.gikk.twirk.types.users.TwitchUser;

import static java.lang.System.out;

public class SpeedySpinGameListener extends CommandBase {

    private final static String pattern = "!preds";
    private final Twirk twirk;
    private SpeedySpinPredictionManager game;

    public SpeedySpinGameListener(Twirk twirk) {
        super(CommandType.PREFIX_COMMAND);
        this.twirk = twirk;
        game = new SpeedySpinPredictionManager(twirk);
    }

    @Override
    protected String getCommandWords() {
        return pattern;
    }

    @Override
    protected USER_TYPE getMinUserPrivilege() {
        return USER_TYPE.OWNER;
    }

    @Override
    protected void performCommand(String command, TwitchUser sender, TwitchMessage message) {
        if (game.isEnabled()) {
            String[] content = message.getContent().split("\\s");
            if (content.length > 0 && content[1].matches("[1-4]{3}")) {
                out.println("Ending the prediction game...");
                game.stop(
                        Badge.values()[content[1].charAt(0)],
                        Badge.values()[content[1].charAt(1)],
                        Badge.values()[content[1].charAt(2)]
                );
            }
        }
        else {
            game = new SpeedySpinPredictionManager(twirk);
            out.println("Starting the prediction game...");
            game.start();
        }
    }
}
