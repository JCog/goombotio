package dev.jcog.goombotio.listeners.events;

import com.github.twitch4j.eventsub.events.ChannelRaidEvent;
import com.netflix.hystrix.exception.HystrixRuntimeException;
import dev.jcog.goombotio.listeners.TwitchEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import dev.jcog.goombotio.util.CommonUtils;
import dev.jcog.goombotio.util.TwitchApi;

public class ShoutoutListener implements TwitchEventListener {
    private static final Logger log = LoggerFactory.getLogger(ShoutoutListener.class);

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
            log.error("Error checking if streamer follows {} after raid: {}", raiderName, e.getMessage());
            return;
        }
        if (streamerFollows && online) {
            twitchApi.shoutout(raiderId);
        }
    }
}
