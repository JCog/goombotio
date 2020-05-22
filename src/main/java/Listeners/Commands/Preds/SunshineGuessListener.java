package Listeners.Commands.Preds;

import Functions.Preds.SunshinePredsManager;
import com.gikk.twirk.events.TwirkListener;
import com.gikk.twirk.types.twitchMessage.TwitchMessage;
import com.gikk.twirk.types.users.TwitchUser;

public class SunshineGuessListener implements TwirkListener {
    private SunshinePredsManager manager;
    private boolean enabled;
    
    public SunshineGuessListener() {
        enabled = false;
    }
    
    public void start(SunshinePredsManager manager) {
        this.manager = manager;
        enabled = true;
    }
    
    public void stop() {
        manager = null;
        enabled = false;
    }
    
    @Override
    public void onPrivMsg(TwitchUser sender, TwitchMessage message) {
        if (enabled) {
            String guess = message.getContent().replaceAll("[^0-9]", "");
            if (guess.length() == 5) {
                int secondDigit = Character.getNumericValue(guess.charAt(1));
                if (secondDigit > 5) {
                    return;
                }
                int minutes = Character.getNumericValue(guess.charAt(0));
                int seconds = Integer.parseInt(guess.substring(1, 3));
                int hundredths = Integer.parseInt(guess.substring(3, 5)) + (seconds * 100) + (minutes * 60 * 100);
                System.out.println(String.format("%s has predicted %d hundredths", sender.getDisplayName(), hundredths));
                manager.makePrediction(sender, hundredths);
            }
        }
    }
}
