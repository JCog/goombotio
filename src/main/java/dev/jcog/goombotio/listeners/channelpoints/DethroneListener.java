package dev.jcog.goombotio.listeners.channelpoints;

import com.github.twitch4j.eventsub.domain.RedemptionStatus;
import com.github.twitch4j.eventsub.domain.Reward;
import com.github.twitch4j.eventsub.events.ChannelPointsCustomRewardRedemptionEvent;
import com.github.twitch4j.eventsub.events.CustomRewardRedemptionAddEvent;
import com.github.twitch4j.helix.domain.ChannelVip;
import com.github.twitch4j.helix.domain.CustomReward;
import com.netflix.hystrix.exception.HystrixRuntimeException;
import dev.jcog.goombotio.database.misc.VipDb;
import dev.jcog.goombotio.listeners.TwitchEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import dev.jcog.goombotio.util.CommonUtils;
import dev.jcog.goombotio.util.TwitchApi;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static dev.jcog.goombotio.listeners.TwitchEventListener.EVENT_TYPE.CHANNEL_POINTS_REDEMPTION;

public class DethroneListener implements TwitchEventListener {
    private static final Logger log = LoggerFactory.getLogger(DethroneListener.class);
    private static final String DETHRONE_REWARD_TITLE = "Dethrone";
    private static final String DETHRONE_REWARD_PROMPT = " currently sits on the throne. Redeem this to take their spot, earn VIP, and increase the cost for the next person!";

    private final TwitchApi twitchApi;
    private final VipDb vipDb;
    
    public DethroneListener(CommonUtils commonUtils) {
        twitchApi = commonUtils.twitchApi();
        vipDb = commonUtils.dbManager().getVipDb();
    }

    @Override
    public List<EVENT_TYPE> getEventTypes() {
        return List.of(CHANNEL_POINTS_REDEMPTION);
    }

    @Override
    public void onChannelPointsRedemption(CustomRewardRedemptionAddEvent event) {
        Reward channelPointsReward = event.getReward();
        if (channelPointsReward.getTitle().startsWith(DETHRONE_REWARD_TITLE)) {
            handleDethroneReward(event);
        }
    }
    
    
    private void handleDethroneReward(ChannelPointsCustomRewardRedemptionEvent event) {
        Reward channelPointsReward = event.getReward();
        CustomReward customReward;
        try {
            customReward = twitchApi.getCustomRewards(
                    twitchApi.getStreamerUser().getId(),
                    Collections.singletonList(channelPointsReward.getId()),
                    true
            ).get(0);
        } catch (HystrixRuntimeException e) {
            log.error("Error retrieving Dethrone reward from API: {}", e.getMessage());
            twitchApi.channelMessage("@JCog error retrieving reward. Please refund manually while shaking your fist at twitch.");
            return;
        }
        
        String oldUsername = channelPointsReward.getTitle().split("\\s")[1];
        String newUsername = event.getUserName();
        int newCost = getNextIncreasedCost(customReward.getCost());
        CustomReward newReward = customReward
                .withCost(newCost)
                .withTitle(DETHRONE_REWARD_TITLE + " " + newUsername)
                .withPrompt(newUsername + DETHRONE_REWARD_PROMPT);
        boolean success;
        try {
            twitchApi.updateCustomReward(twitchApi.getStreamerUser().getId(), channelPointsReward.getId(), newReward);
            success = true;
        } catch (HystrixRuntimeException e) {
            success = false;
            log.error("Error updating Dethrone reward. Refunding points: {}", e.getMessage());
            twitchApi.channelMessage("Error updating Dethrone reward. Refunding points.");
        }
        
        try {
            twitchApi.updateRedemptionStatus(
                    twitchApi.getStreamerUser().getId(),
                    channelPointsReward.getId(),
                    Collections.singletonList(event.getId()),
                    success ? RedemptionStatus.FULFILLED : RedemptionStatus.CANCELED
            );
        } catch (HystrixRuntimeException e) {
            log.error(e.getMessage());
            twitchApi.channelMessage(String.format("@JCog error %s Dethrone reward. Please do so manually while shaking your fist at twitch.", success ? "fulfilling" : "refunding"));
        }
        
        if (!success) {
            return;
        }
        
        twitchApi.channelMessage(String.format("%s has taken the throne from %s! The cost to dethrone them has increased to %d. jcogBan", newUsername, oldUsername, newCost));
        
        // update VIPs
        List<String> vipIds;
        try {
            vipIds = twitchApi.getChannelVips().stream().map(ChannelVip::getUserId).collect(Collectors.toList());
        } catch (HystrixRuntimeException e) {
            log.error(e.getMessage());
            twitchApi.channelMessage("API error getting current VIPs");
            return;
        }
        
        String newUserId = event.getUserId();
        String oldUserId = vipDb.getThroneUserId();
        vipDb.editThroneProp(newUserId, true);
        if (oldUserId == null) {
            twitchApi.channelMessage("Error finding existing throne holder ID. Unable to remove VIP.");
            log.error("Error finding existing throne holder ID. Unable to remove VIP.");
        } else {
            vipDb.editThroneProp(oldUserId, false);
        }
        
        // add new VIP
        if (!vipIds.contains(newUserId)) {
            try {
                twitchApi.vipAdd(newUserId);
                log.info("VIP added to user {}", newUserId);
            } catch (HystrixRuntimeException e) {
                twitchApi.channelMessage("API error adding new VIP");
                log.error("error adding {} ({}) as VIP: {}", newUsername, newUserId, e.getMessage());
            }
        }
        
        // remove old VIP
        if (oldUserId != null && !vipDb.hasVip(oldUserId)) {
            try {
                twitchApi.vipRemove(oldUserId);
                log.info("VIP removed from user {}", newUserId);
            } catch (HystrixRuntimeException e) {
                twitchApi.channelMessage("API error removing old VIP");
                log.error("error removing {} ({}) as VIP: {}", oldUsername, oldUserId, e.getMessage());
            }
        }
    }
    
    private int getNextIncreasedCost(int prevCost) {
        return getQuadraticCost(getQuadraticIndex(prevCost) + 1);
    }
    
    private int getQuadraticIndex(int cost) {
        // (-85 ± sqrt(85^2 - 20(910-cost))) / 10
        int temp = (int) Math.sqrt(7225 - 20 * (910 - cost));
        int first = (-85 + temp) / 10;
        int second = (-85 - temp) / 10;
        return Math.max(first, second);
    }
    
    private int getQuadraticCost(int index) {
        return 5*index*index + 85*index + 910;
    }
}
