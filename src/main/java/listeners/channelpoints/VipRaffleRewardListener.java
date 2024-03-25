package listeners.channelpoints;

import com.github.twitch4j.eventsub.domain.RedemptionStatus;
import com.github.twitch4j.helix.domain.Moderator;
import com.github.twitch4j.pubsub.domain.ChannelPointsReward;
import com.github.twitch4j.pubsub.events.RewardRedeemedEvent;
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
    private static final String RAFFLE_REWARD_TITLE = "VIP Raffle Entries";
    private static final int ENTRY_COUNT = 4;
    
    private final TwitchApi twitchApi;
    private final VipDb vipDb;
    private final VipRaffleDb vipRaffleDb;
    
    public VipRaffleRewardListener(CommonUtils commonUtils) {
        twitchApi = commonUtils.getTwitchApi();
        vipDb = commonUtils.getDbManager().getVipDb();
        vipRaffleDb = commonUtils.getDbManager().getVipRaffleDb();
    }
    
    @Override
    public void onChannelPointsRedemption(RewardRedeemedEvent event) {
        ChannelPointsReward channelPointsReward = event.getRedemption().getReward();
        if (channelPointsReward.getTitle().startsWith(RAFFLE_REWARD_TITLE)) {
            handleRaffleEntryAddition(event);
        }
    }
    
    
    private void handleRaffleEntryAddition(RewardRedeemedEvent event) {
        String userId = event.getRedemption().getUser().getId();
        String displayName = event.getRedemption().getUser().getDisplayName();
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
                entryCount = vipRaffleItem.getEntryCount();
                twitchApi.channelMessage(String.format("@%s You now have %d entr%s!",
                        displayName,
                        entryCount,
                        entryCount == 1 ? "y" : "ies"
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
                    event.getRedemption().getChannelId(),
                    event.getRedemption().getReward().getId(),
                    Collections.singletonList(event.getRedemption().getId()),
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
