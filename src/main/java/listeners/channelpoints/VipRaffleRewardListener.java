package listeners.channelpoints;

import com.github.twitch4j.eventsub.domain.RedemptionStatus;
import com.github.twitch4j.pubsub.domain.ChannelPointsReward;
import com.github.twitch4j.pubsub.events.RewardRedeemedEvent;
import com.netflix.hystrix.exception.HystrixRuntimeException;
import database.DbManager;
import database.entries.VipRaffleItem;
import database.misc.VipRaffleDb;
import listeners.TwitchEventListener;
import util.TwitchApi;

import java.util.Collections;

import static java.lang.System.out;

public class VipRaffleRewardListener implements TwitchEventListener {
    private static final String RAFFLE_REWARD_TITLE = "VIP Raffle Entry";
    private static final int ENTRY_COUNT = 4;
    
    
    private final TwitchApi twitchApi;
    private final VipRaffleDb vipRaffleDb;
    
    public VipRaffleRewardListener(TwitchApi twitchApi, DbManager dbManager) {
        this.twitchApi = twitchApi;
        this.vipRaffleDb = dbManager.getVipRaffleDb();
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
    
        // increment entry count in database
        vipRaffleDb.incrementEntryCount(userId, ENTRY_COUNT);
        VipRaffleItem vipRaffleItem = vipRaffleDb.getVipRaffleItem(userId);
        boolean success;
        int entryCount;
        if (vipRaffleItem != null) {
            entryCount = vipRaffleItem.getEntryCount();
            twitchApi.channelMessage(String.format("@%s You now have %d entr%s!",
                    username,
                    entryCount,
                    entryCount == 1 ? "y" : "ies"
            ));
            success = true;
        } else {
            out.printf("Error getting entry total for %s%n", username);
            success = false;
        }
    
        // mark reward as fulfilled or canceled on Twitch
        try {
            twitchApi.updateRedemptionStatus(
                    event.getRedemption().getChannelId(),
                    event.getRedemption().getReward().getId(),
                    Collections.singletonList(event.getRedemption().getId()),
                    success ? RedemptionStatus.FULFILLED : RedemptionStatus.CANCELED
            );
        } catch (HystrixRuntimeException e) {
            twitchApi.channelMessage(
                    String.format(
                            "@JCog error %s Raffle reward. Please do so manually while shaking your fist at twitch.",
                            success ? "fulfilling" : "refunding"
                    )
            );
        }
    }
}
