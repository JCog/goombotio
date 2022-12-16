package listeners.channelpoints;

import com.github.twitch4j.eventsub.domain.RedemptionStatus;
import com.github.twitch4j.helix.domain.Moderator;
import com.github.twitch4j.helix.domain.User;
import com.github.twitch4j.pubsub.domain.ChannelPointsReward;
import com.github.twitch4j.pubsub.events.RewardRedeemedEvent;
import com.netflix.hystrix.exception.HystrixRuntimeException;
import database.DbManager;
import database.entries.VipRaffleItem;
import database.misc.PermanentVipsDb;
import database.misc.VipRaffleDb;
import listeners.TwitchEventListener;
import util.TwitchApi;

import java.util.Collections;
import java.util.HashSet;
import java.util.stream.Collectors;

import static java.lang.System.out;

public class VipRaffleRewardListener implements TwitchEventListener {
    private static final String RAFFLE_REWARD_TITLE = "VIP Raffle Entries";
    private static final int ENTRY_COUNT = 4;
    
    private final TwitchApi twitchApi;
    private final PermanentVipsDb permanentVipsDb;
    private final VipRaffleDb vipRaffleDb;
    private final User streamerUser;
    
    public VipRaffleRewardListener(TwitchApi twitchApi, DbManager dbManager, User streamerUser) {
        this.twitchApi = twitchApi;
        this.permanentVipsDb = dbManager.getPermanentVipsDb();
        this.vipRaffleDb = dbManager.getVipRaffleDb();
        this.streamerUser = streamerUser;
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
        String username = event.getRedemption().getUser().getDisplayName();
        HashSet<String> modIds = twitchApi.getMods(streamerUser.getId())
                .stream()
                .map(Moderator::getUserId)
                .collect(Collectors.toCollection(HashSet::new));
    
        boolean shouldFulfill;
        // mods, permanent VIPs, and the streamer are all ineligible for the VIP raffle
        if (modIds.contains(userId) || permanentVipsDb.isPermanentVip(userId) || userId.equals(streamerUser.getId())) {
            twitchApi.channelMessage(String.format(
                    "@%s You're not eligible for the VIP raffle. Your points will be refunded.",
                    username
            ));
            shouldFulfill = false;
        } else {
            // increment entry count in database
            vipRaffleDb.incrementEntryCount(userId, ENTRY_COUNT);
            VipRaffleItem vipRaffleItem = vipRaffleDb.getVipRaffleItem(userId);
            int entryCount;
            if (vipRaffleItem != null) {
                entryCount = vipRaffleItem.getEntryCount();
                twitchApi.channelMessage(String.format("@%s You now have %d entr%s!",
                        username,
                        entryCount,
                        entryCount == 1 ? "y" : "ies"
                ));
                shouldFulfill = true;
            } else {
                out.printf("Error getting entry total for %s%n", username);
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
