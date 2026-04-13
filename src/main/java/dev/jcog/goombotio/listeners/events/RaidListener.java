package dev.jcog.goombotio.listeners.events;

import com.github.twitch4j.eventsub.events.ChannelRaidEvent;
import com.github.twitch4j.helix.domain.AdSchedule;
import com.netflix.hystrix.exception.HystrixRuntimeException;
import dev.jcog.goombotio.listeners.TwitchEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import dev.jcog.goombotio.util.CommonUtils;
import dev.jcog.goombotio.util.TwitchApi;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static dev.jcog.goombotio.listeners.TwitchEventListener.EVENT_TYPE.RAID;

public class RaidListener implements TwitchEventListener {
    private static final Logger log = LoggerFactory.getLogger(RaidListener.class);

    private final TwitchApi twitchApi;

    public RaidListener(CommonUtils commonUtils) {
        twitchApi = commonUtils.twitchApi();
    }

    @Override
    public List<EVENT_TYPE> getEventTypes() {
        return List.of(RAID);
    }

    @Override
    public void onRaid(ChannelRaidEvent raidEvent) {
        String streamerId = raidEvent.getToBroadcasterUserId();
        String raiderId = raidEvent.getFromBroadcasterUserId();
        String raiderName = raidEvent.getFromBroadcasterUserName();
        boolean streamerFollows = false;
        boolean online = false;
        try {
            // only shoutout users the streamer follows
            streamerFollows = twitchApi.getFollowedChannel(streamerId, raiderId) != null;
            online = twitchApi.getStreamByUserId(twitchApi.getStreamerUser().getId()) != null;
        } catch (HystrixRuntimeException e) {
            log.error("Error checking if streamer follows {} after raid: {}", raiderName, e.getMessage());
        }
        if (streamerFollows && online) {
            twitchApi.shoutout(raiderId);
        }

        AdSchedule adSchedule = null;
        try {
            adSchedule = twitchApi.getAdSchedule();
        } catch (HystrixRuntimeException e) {
            log.error("Error getting ad schedule: {}", e.getMessage());
        }
        if (adSchedule != null && raidEvent.getViewers() > 1 && adSchedule.getSnoozeCount() > 0) {
            long mins = ChronoUnit.MINUTES.between(Instant.now(), adSchedule.getNextAdAt());
            if (mins < 5) {
                if (twitchApi.snoozeNextAd()) {
                    log.info("Snoozed ad scheduled {} minutes after raid", mins);
                } else {
                    log.warn("Unable to snooze ad scheduled {} minutes after raid", mins);
                }
            }
        }
    }
}
