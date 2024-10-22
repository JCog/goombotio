package listeners.channelpoints;

import com.github.twitch4j.eventsub.domain.RedemptionStatus;
import com.github.twitch4j.eventsub.domain.Reward;
import com.github.twitch4j.eventsub.events.ChannelPointsCustomRewardRedemptionEvent;
import com.github.twitch4j.eventsub.events.CustomRewardRedemptionAddEvent;
import com.github.twitch4j.helix.domain.Moderator;
import com.netflix.hystrix.exception.HystrixRuntimeException;
import database.misc.VipDb;
import database.misc.VipRaffleDb;
import listeners.TwitchEventListener;
import util.CommonUtils;
import util.TwitchApi;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import static database.misc.VipRaffleDb.VipRaffleItem;
import static java.lang.System.out;

public class VipRaffleRewardListener implements TwitchEventListener {
    private static final String RAFFLE_REWARD_TITLE = "VIP Raffle Entry";
    private static final int ENTRY_COUNT = 1;
    
    private final TwitchApi twitchApi;
    private final VipDb vipDb;
    private final VipRaffleDb vipRaffleDb;
    
    public VipRaffleRewardListener(CommonUtils commonUtils) {
        twitchApi = commonUtils.twitchApi();
        vipDb = commonUtils.dbManager().getVipDb();
        vipRaffleDb = commonUtils.dbManager().getVipRaffleDb();
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
                twitchApi.channelMessage(String.format("@%s You now have %d entr%s! (~%.1f%% of all entries)",
                        displayName,
                        entryCount,
                        entryCount == 1 ? "y" : "ies",
                        (float) entryCount * 100 / vipRaffleDb.getTotalEntryCountCurrentMonth()
                ));
                shouldFulfill = true;
            } else {
                out.printf("Error getting entry total for %s%n", displayName);
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
}
