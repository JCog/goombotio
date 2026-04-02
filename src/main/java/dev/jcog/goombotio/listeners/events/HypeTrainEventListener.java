package dev.jcog.goombotio.listeners.events;

import com.github.twitch4j.eventsub.events.HypeTrainBeginV2Event;
import dev.jcog.goombotio.listeners.TwitchEventListener;
import dev.jcog.goombotio.util.CommonUtils;
import dev.jcog.goombotio.util.TwitchApi;

import java.util.List;

import static dev.jcog.goombotio.listeners.TwitchEventListener.EVENT_TYPE.HYPE_TRAIN_BEGIN;

public class HypeTrainEventListener implements TwitchEventListener {
    private final TwitchApi twitchApi;

    public HypeTrainEventListener(CommonUtils commonUtils) {
        twitchApi = commonUtils.twitchApi();
    }

    @Override
    public List<EVENT_TYPE> getEventTypes() {
        return List.of(HYPE_TRAIN_BEGIN);
    }

    @Override
    public void onHypeTrainBegin(HypeTrainBeginV2Event hypeTrainBeginEvent) {
        twitchApi.channelAnnouncement("ScamTrain Scam train!! ScamTrain");
    }
}
