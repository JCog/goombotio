package dev.jcog.goombotio.listeners.events;

import com.github.twitch4j.eventsub.events.HypeTrainBeginV2Event;
import dev.jcog.goombotio.listeners.TwitchEventListener;
import dev.jcog.goombotio.util.CommonUtils;
import dev.jcog.goombotio.util.TwitchApi;

public class HypeTrainEventListener implements TwitchEventListener {
    private final TwitchApi twitchApi;

    public HypeTrainEventListener(CommonUtils commonUtils) {
        twitchApi = commonUtils.twitchApi();
    }
    
    @Override
    public void onHypeTrainBegin(HypeTrainBeginV2Event hypeTrainBeginEvent) {
        twitchApi.channelAnnouncement("ScamTrain Scam train!! ScamTrain");
    }
}
