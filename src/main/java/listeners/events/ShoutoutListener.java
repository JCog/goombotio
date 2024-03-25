package listeners.events;

import com.github.twitch4j.chat.events.channel.RaidEvent;
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
    public void onRaid(RaidEvent raidEvent) {
        String streamerId = twitchApi.getStreamerUser().getId();
        String raiderId = raidEvent.getRaider().getId();
        boolean streamerFollows;
        try {
            // only shoutout users the streamer follows
            streamerFollows = twitchApi.getFollowedChannel(streamerId, raiderId) != null;
        } catch (HystrixRuntimeException e) {
            System.out.printf("Error checking if streamer follows %s after raid.%n", raidEvent.getRaider().getName());
            return;
        }
        if (streamerFollows) {
            twitchApi.shoutout(raidEvent.getRaider().getId());
        }
    }
}
