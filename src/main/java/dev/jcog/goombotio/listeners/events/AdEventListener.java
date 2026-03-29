package dev.jcog.goombotio.listeners.events;

import com.github.twitch4j.eventsub.events.ChannelAdBreakBeginEvent;
import dev.jcog.goombotio.listeners.TwitchEventListener;
import dev.jcog.goombotio.util.CommonUtils;
import dev.jcog.goombotio.util.TwitchApi;

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
