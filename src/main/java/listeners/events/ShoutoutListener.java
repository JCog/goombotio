package listeners.events;

import com.github.twitch4j.eventsub.events.ChannelRaidEvent;
import com.netflix.hystrix.exception.HystrixRuntimeException;
import listeners.TwitchEventListener;
import util.CommonUtils;
import util.TwitchApi;

public class ShoutoutListener implements TwitchEventListener {
    private final TwitchApi twitchApi;

    public ShoutoutListener(CommonUtils commonUtils) {
        twitchApi = commonUtils.twitchApi();
    }
    
    @Override
    public void onRaid(ChannelRaidEvent raidEvent) {
        String streamerId = raidEvent.getToBroadcasterUserId();
        String raiderId = raidEvent.getFromBroadcasterUserId();
        String raiderName = raidEvent.getFromBroadcasterUserName();
        boolean streamerFollows;
        boolean online;
        try {
            // only shoutout users the streamer follows
            streamerFollows = twitchApi.getFollowedChannel(streamerId, raiderId) != null;
            online = twitchApi.getStreamByUserId(twitchApi.getStreamerUser().getId()) != null;
        } catch (HystrixRuntimeException e) {
            System.out.printf("Error checking if streamer follows %s after raid.%n", raiderName);
            return;
        }
        if (streamerFollows && online) {
            twitchApi.shoutout(raiderId);
        }
    }
}
