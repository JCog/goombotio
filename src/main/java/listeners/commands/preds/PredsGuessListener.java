package listeners.commands.preds;

import com.github.twitch4j.chat.events.channel.ChannelMessageEvent;
import functions.preds.PredsManagerBase;
import listeners.TwitchEventListener;

public class PredsGuessListener implements TwitchEventListener {
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
    public void onChannelMessage(ChannelMessageEvent messageEvent) {
        if (enabled) {
            String content = messageEvent.getMessage().trim();
            String userId = messageEvent.getUser().getId();
            String displayName = TwitchEventListener.getDisplayName(messageEvent.getMessageEvent().getTags());
            manager.makePredictionIfValid(userId, displayName, content);
        }
    }
}
