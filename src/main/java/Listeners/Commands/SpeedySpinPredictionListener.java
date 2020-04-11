package Listeners.Commands;

import Functions.SpeedySpinPredictionManager;
import com.gikk.twirk.events.TwirkListener;
import com.gikk.twirk.types.twitchMessage.TwitchMessage;
import com.gikk.twirk.types.users.TwitchUser;

import java.util.*;

import static Functions.SpeedySpinPredictionManager.*;

public class SpeedySpinPredictionListener implements TwirkListener {
    
    private static final Set<String> BADGE_CHOICES = new HashSet<>(Arrays.asList(
            "badspin1",
            "badspin2",
            "badspin3",
            "spoodlyspun"
    ));

    private final SpeedySpinPredictionManager manager;

    public SpeedySpinPredictionListener(SpeedySpinPredictionManager manager) {
        this.manager = manager;
    }

    @Override
    public void onPrivMsg(TwitchUser sender, TwitchMessage message) {
        String content = message.getContent().trim();
        String[] split = content.split("\\s");

        if (split.length == 3) {
            //using FFZ emotes
            ArrayList<String> badgeGuess = new ArrayList<>();
            for (String word : split) {
                if (BADGE_CHOICES.contains(word.toLowerCase()) && !badgeGuess.contains(word.toLowerCase())) {
                    badgeGuess.add(word.toLowerCase());
                }
            }
    
            if (badgeGuess.size() == 3) {
                manager.makePrediction(
                        sender,
                        stringToBadge(badgeGuess.get(0)),
                        stringToBadge(badgeGuess.get(1)),
                        stringToBadge(badgeGuess.get(2))
                );
                System.out.println(String.format("%s has predicted %s %s %s",
                        sender.getUserName(), badgeGuess.get(0), badgeGuess.get(1), badgeGuess.get(2)));
            }
        }
        else if (split.length == 1 && split[0].matches("[1-4]{3}")) {
            //using numbers, e.g. "412"
            Vector<Integer> badgeGuess = new Vector<>();
            for (int i = 0; i < split[0].length(); i++) {
                int guess = Character.getNumericValue(split[0].charAt(i));
                if (!badgeGuess.contains(guess)) {
                    badgeGuess.add(guess);
                }
            }
            
            if (badgeGuess.size() == 3) {
                Badge badge1 = intToBadge(badgeGuess.get(0));
                Badge badge2 = intToBadge(badgeGuess.get(1));
                Badge badge3 = intToBadge(badgeGuess.get(2));
                
                manager.makePrediction(sender, badge1, badge2, badge3);
                
                System.out.println(String.format("%s has predicted %s %s %s", sender.getUserName(),
                        badgeToString(badge1), badgeToString(badge2), badgeToString(badge3)));
            }
        }
    }
}
