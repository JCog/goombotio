package listeners.events;

import com.github.twitch4j.eventsub.events.ChannelRaidEvent;
import com.netflix.hystrix.exception.HystrixRuntimeException;
import listeners.TwitchEventListener;
import util.CommonUtils;
import util.TwitchApi;

public class ShoutoutListener implements TwitchEventListener {
    private final TwitchApi twitchApi;

    public ShoutoutListener(CommonUtils commonUtils) {
        twitchApi = commonUtils.getTwitchApi();
    }
    
    @Override
    public void onRaid(ChannelRaidEvent raidEvent) {
        String streamerId = twitchApi.getStreamerUser().getId();
        String raiderId = raidEvent.getFromBroadcasterUserId();
        String raiderName = raidEvent.getFromBroadcasterUserName();
        boolean streamerFollows;
        try {
            // only shoutout users the streamer follows
            streamerFollows = twitchApi.getFollowedChannel(streamerId, raiderId) != null;
        } catch (HystrixRuntimeException e) {
            System.out.printf("Error checking if streamer follows %s after raid.%n", raiderName);
            return;
        }
        if (streamerFollows) {
            twitchApi.shoutout(raiderId);
        }
    }
}
