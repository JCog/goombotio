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
                "A scheduled ad break has just started. I run ads both to disable pre-rolls when you enter chat and " +
                "for the revenue that helps keep me streaming for you. If you would like to avoid seeing them in the " +
                "future, consider subscribing to the channel or purchasing Twitch Turbo! Use !sub or !turbo for more " +
                "details. Adge"
        );
    }
}
