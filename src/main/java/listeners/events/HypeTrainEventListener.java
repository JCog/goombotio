package listeners.events;

import com.github.twitch4j.eventsub.events.HypeTrainBeginEvent;
import listeners.TwitchEventListener;
import util.CommonUtils;
import util.TwitchApi;

public class HypeTrainEventListener implements TwitchEventListener {
    private final TwitchApi twitchApi;

    public HypeTrainEventListener(CommonUtils commonUtils) {
        twitchApi = commonUtils.twitchApi();
    }
    
    @Override
    public void onHypeTrainBegin(HypeTrainBeginEvent hypeTrainBeginEvent) {
        twitchApi.channelAnnouncement("ScamTrain Scam train!! ScamTrain ");
    }
}
