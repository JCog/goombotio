package dev.jcog.goombotio.listeners.channelpoints;

import com.github.twitch4j.eventsub.domain.RedemptionStatus;
import com.github.twitch4j.eventsub.domain.Reward;
import com.github.twitch4j.eventsub.events.ChannelPointsCustomRewardRedemptionEvent;
import com.github.twitch4j.eventsub.events.CustomRewardRedemptionAddEvent;
import com.github.twitch4j.helix.domain.Moderator;
import com.netflix.hystrix.exception.HystrixRuntimeException;
import dev.jcog.goombotio.database.misc.VipDb;
import dev.jcog.goombotio.database.misc.VipRaffleDb;
import dev.jcog.goombotio.listeners.TwitchEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import dev.jcog.goombotio.util.CommonUtils;
import dev.jcog.goombotio.util.TwitchApi;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

import static dev.jcog.goombotio.database.misc.VipRaffleDb.VipRaffleItem;
import static dev.jcog.goombotio.listeners.TwitchEventListener.EVENT_TYPE.CHANNEL_POINTS_REDEMPTION;

public class VipRaffleRewardListener implements TwitchEventListener {
    private static final Logger log = LoggerFactory.getLogger(VipRaffleRewardListener.class);
    private static final String RAFFLE_REWARD_TITLE = "VIP Raffle Entry";
    private static final int ENTRY_COUNT = 1;
    private static final int COOLDOWN_PERIOD = 60; // seconds
    private static final int COOLDOWN_TRIGGER = 4;

    private final TwitchApi twitchApi;
    private final VipDb vipDb;
    private final VipRaffleDb vipRaffleDb;
    private final Deque<Instant> timestamps = new LinkedList<>();
    
    private boolean cooldownActive = false;
    
    public VipRaffleRewardListener(CommonUtils commonUtils) {
        twitchApi = commonUtils.twitchApi();
        vipDb = commonUtils.dbManager().getVipDb();
        vipRaffleDb = commonUtils.dbManager().getVipRaffleDb();
    }

    @Override
    public List<EVENT_TYPE> getEventTypes() {
        return List.of(CHANNEL_POINTS_REDEMPTION);
    }

    @Override
    public void onChannelPointsRedemption(CustomRewardRedemptionAddEvent event) {
        Reward channelPointsReward = event.getReward();
        if (channelPointsReward.getTitle().startsWith(RAFFLE_REWARD_TITLE)) {
            handleRaffleEntryAddition(event);
        }
    }
    
    
    private void handleRaffleEntryAddition(ChannelPointsCustomRewardRedemptionEvent event) {
        String userId = event.getUserId();
        String displayName = event.getUserName();
        Set<String> modIds = twitchApi.getMods(twitchApi.getStreamerUser().getId())
                .stream()
                .map(Moderator::getUserId)
                .collect(Collectors.toSet());
    
        boolean shouldFulfill;
        // mods, permanent VIPs, and the streamer are all ineligible for the VIP raffle
        if (modIds.contains(userId) || vipDb.isPermanentVip(userId) || userId.equals(twitchApi.getStreamerUser().getId())) {
            twitchApi.channelMessage(String.format(
                    "@%s You're not eligible for the VIP raffle. Your points will be refunded.",
                    displayName
            ));
            shouldFulfill = false;
        } else {
            // increment entry count in database
            vipRaffleDb.incrementEntryCount(userId, displayName, ENTRY_COUNT);
            VipRaffleItem vipRaffleItem = vipRaffleDb.getVipRaffleItem(userId);
            int entryCount;
            if (vipRaffleItem != null) {
                entryCount = vipRaffleItem.entryCount();
                shouldFulfill = true;
                if (!checkForCooldown()) {
                    twitchApi.channelMessage(String.format("@%s You now have %d entr%s! (~%.1f%% of all entries)",
                            displayName,
                            entryCount,
                            entryCount == 1 ? "y" : "ies",
                            (float) entryCount * 100 / vipRaffleDb.getTotalEntryCountCurrentMonth()
                    ));
                }
            } else {
                log.error("Error getting entry total for {}", displayName);
                shouldFulfill = false;
            }
        }
    
        // mark reward as fulfilled or canceled on Twitch
        try {
            twitchApi.updateRedemptionStatus(
                    event.getBroadcasterUserId(),
                    event.getReward().getId(),
                    Collections.singletonList(event.getId()),
                    shouldFulfill ? RedemptionStatus.FULFILLED : RedemptionStatus.CANCELED
            );
        } catch (HystrixRuntimeException e) {
            twitchApi.channelMessage(
                    String.format(
                            "@JCog error %s Raffle reward. Please do so manually while shaking your fist at twitch.",
                            shouldFulfill ? "fulfilling" : "refunding"
                    )
            );
        }
    }
    
    private boolean checkForCooldown() {
        timestamps.addLast(Instant.now());
        if (timestamps.size() < COOLDOWN_TRIGGER) {
            return false;
        }
        
        if (ChronoUnit.SECONDS.between(timestamps.pollFirst(), timestamps.getLast()) < COOLDOWN_PERIOD) {
            if (!cooldownActive) {
                twitchApi.channelAnnouncement(
                        "Temporarily suppressing raffle output to prevent spam. Don't worry, your raffle entries are " +
                        "still being recorded!"
                );
            }
            cooldownActive = true;
            return true;
        } else {
            cooldownActive = false;
            return false;
        }
    }
}
