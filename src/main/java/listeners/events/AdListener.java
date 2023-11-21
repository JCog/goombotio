package listeners.events;

import com.github.twitch4j.pubsub.events.MidrollRequestEvent;
import listeners.TwitchEventListener;
import util.TwitchApi;

public class AdListener implements TwitchEventListener {
    
    private final TwitchApi twitchApi;

    public AdListener(TwitchApi twitchApi) {
        this.twitchApi = twitchApi;
    }
    
    @Override
    public void onMidrollRequest(MidrollRequestEvent midrollRequestEvent) {
    
    }

}
