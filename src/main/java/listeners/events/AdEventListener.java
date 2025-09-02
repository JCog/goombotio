package listeners.events;

import com.github.twitch4j.eventsub.events.ChannelAdBreakBeginEvent;
import listeners.TwitchEventListener;
import util.CommonUtils;
import util.TwitchApi;

public class AdEventListener implements TwitchEventListener {
    private final TwitchApi twitchApi;

    public AdEventListener(CommonUtils commonUtils) {
        twitchApi = commonUtils.twitchApi();
    }
    
    @Override
    public void onAdBegin(ChannelAdBreakBeginEvent adEvent) {
        twitchApi.channelAnnouncement(
                "A scheduled ad break has just started. Ads help me keep streaming, but if you'd like to avoid " +
                "seeing them, consider subscribing to the channel or purchasing Twitch Turbo! Adge"
        );
    }
}
