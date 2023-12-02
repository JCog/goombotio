package listeners.events;

import com.github.twitch4j.chat.events.channel.RaidEvent;
import listeners.TwitchEventListener;
import util.TwitchApi;

public class ShoutoutListener implements TwitchEventListener {
    private final TwitchApi twitchApi;

    public ShoutoutListener(TwitchApi twitchApi) {
        this.twitchApi = twitchApi;
    }
    
    @Override
    public void onRaid(RaidEvent raidEvent) {
        if (raidEvent.getViewers() > 1) {
            twitchApi.shoutout(raidEvent.getRaider().getId());
        }
    }
}
