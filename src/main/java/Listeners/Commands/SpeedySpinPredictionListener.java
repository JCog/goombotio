package Listeners.Commands;

import Functions.SpeedySpinPredictionManager;
import com.gikk.twirk.events.TwirkListener;
import com.gikk.twirk.types.twitchMessage.TwitchMessage;
import com.gikk.twirk.types.users.TwitchUser;

import java.util.*;

public class SpeedySpinPredictionListener implements TwirkListener {
    private static final Set<String> badgeChoices = new HashSet<>(Arrays.asList(
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
        ArrayList<String> badgeGuess = new ArrayList<>();

        if (split.length == 3) {
            for (String word : split) {
                if (badgeChoices.contains(word.toLowerCase()) && !badgeGuess.contains(word.toLowerCase())) {
                    badgeGuess.add(word.toLowerCase());
                }
            }
        }

        if (badgeGuess.size() == 3) {
            manager.makePrediction(
                    sender,
                    SpeedySpinPredictionManager.stringToBadge(badgeGuess.get(0)),
                    SpeedySpinPredictionManager.stringToBadge(badgeGuess.get(1)),
                    SpeedySpinPredictionManager.stringToBadge(badgeGuess.get(2))
            );
            System.out.println(String.format("%s has predicted %s %s %s",
                    sender.getUserName(), badgeGuess.get(0), badgeGuess.get(1), badgeGuess.get(2)));
        }
        
        //TODO: accept guesses using just numbers
    }
}
