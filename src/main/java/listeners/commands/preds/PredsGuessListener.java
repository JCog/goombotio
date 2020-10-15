package listeners.commands.preds;

import com.gikk.twirk.events.TwirkListener;
import com.gikk.twirk.types.twitchMessage.TwitchMessage;
import com.gikk.twirk.types.users.TwitchUser;
import functions.preds.PredsManagerBase;

public class PredsGuessListener implements TwirkListener {
    protected PredsManagerBase manager;
    protected boolean enabled;

    public PredsGuessListener() {
        enabled = false;
    }

    public void start(PredsManagerBase manager) {
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
            String content = message.getContent().trim();
            manager.makePredictionIfValid(sender, content);
        }
    }
}
