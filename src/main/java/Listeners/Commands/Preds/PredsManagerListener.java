package Listeners.Commands.Preds;

import Functions.Preds.PapePredsManager;
import Functions.Preds.PapePredsManager.Badge;
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
    private final PapeGuessListener papeGuessListener;
    private final SunshineGuessListener sunshineGuessListener;
    
    private PapePredsManager papePredsManager;
    private SunshinePredsManager sunshinePredsManager;

    public PredsManagerListener(
            TwirkInterface twirk,
            StreamInfo streamInfo,
            PapeGuessListener papeGuessListener,
            SunshineGuessListener sunshineGuessListener
    ) {
        super(CommandType.PREFIX_COMMAND);
        this.twirk = twirk;
        this.streamInfo = streamInfo;
        this.papeGuessListener = papeGuessListener;
        this.sunshineGuessListener = sunshineGuessListener;
        papePredsManager = new PapePredsManager(twirk);
        sunshinePredsManager = new SunshinePredsManager(twirk);
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
        if (streamInfo.getGame().equals(GAME_PAPER_MARIO)) {
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
        //else if (streamInfo.getGame().equals(GAME_SUNSHINE)) {
        else {
            if (sunshinePredsManager.isEnabled()) {
                if (sunshinePredsManager.isWaitingForAnswer()) {
                    String[] content = message.getContent().split("\\s");
                    if (content.length > 1 && content[1].matches("[0-9]{5}")) {
                        out.println("Submitting predictions...");
                        int outcome = Integer.parseInt(content[1]);
                        int minutes = outcome / 10000;
                        outcome -= minutes * 10000;
                        int seconds = outcome / 100;
                        outcome -= seconds * 100;
                        
                        int hundredths = outcome + (seconds * 100) + (minutes * 60 * 100);
                        sunshinePredsManager.submitPredictions(hundredths);
                    }
                }
                else {
                    out.println("Ending the prediction game...");
                    sunshineGuessListener.stop();
                    sunshinePredsManager.stop();
                }
            }
            else {
                sunshinePredsManager = new SunshinePredsManager(twirk);
                sunshineGuessListener.start(sunshinePredsManager);
                out.println("Starting the prediction game...");
                sunshinePredsManager.start();
            }
        }
    }
}
