package listeners.events;

import com.github.twitch4j.pubsub.events.MidrollRequestEvent;
import listeners.TwitchEventListener;
import util.TwitchApi;

public class AdEventListener implements TwitchEventListener {
    private final TwitchApi twitchApi;

    public AdEventListener(TwitchApi twitchApi) {
        this.twitchApi = twitchApi;
    }
    
    @Override
    public void onMidrollRequest(MidrollRequestEvent midrollRequestEvent) {
        twitchApi.channelAnnouncement(
                "A scheduled ad break has just started. I run 3 minutes of ads per hour both to disable pre-rolls " +
                "when you enter chat and for the revenue that helps keep me streaming for you. If you would like to " +
                "avoid seeing them in the future, consider subscribing to the channel or purchasing Twitch Turbo! " +
                "Use !sub or !turbo for more details. Adge"
        );
    }
}
