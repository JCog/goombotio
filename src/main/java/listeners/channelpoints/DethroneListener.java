package listeners.channelpoints;

import com.github.twitch4j.eventsub.domain.RedemptionStatus;
import com.github.twitch4j.eventsub.domain.Reward;
import com.github.twitch4j.eventsub.events.ChannelPointsCustomRewardRedemptionEvent;
import com.github.twitch4j.eventsub.events.CustomRewardRedemptionAddEvent;
import com.github.twitch4j.helix.domain.ChannelVip;
import com.github.twitch4j.helix.domain.CustomReward;
import com.netflix.hystrix.exception.HystrixRuntimeException;
import database.misc.VipDb;
import listeners.TwitchEventListener;
import util.CommonUtils;
import util.TwitchApi;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static java.lang.System.out;

public class DethroneListener implements TwitchEventListener {
    private static final String DETHRONE_REWARD_TITLE = "Dethrone";
    private static final String DETHRONE_REWARD_PROMPT = " currently sits on the throne. Redeem this to take their spot, earn VIP, and increase the cost for the next person!";
    
    private final TwitchApi twitchApi;
    private final VipDb vipDb;
    
    public DethroneListener(CommonUtils commonUtils) {
        twitchApi = commonUtils.twitchApi();
        vipDb = commonUtils.dbManager().getVipDb();
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
            out.println("Error retrieving Dethrone reward from API");
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
            out.println("Error updating Dethrone reward. Refunding points.");
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
            twitchApi.channelMessage("API error getting current VIPs");
            return;
        }
        
        String newUserId = event.getUserId();
        String oldUserId = vipDb.getThroneUserId();
        vipDb.editThroneProp(newUserId, true);
        if (oldUserId == null) {
            twitchApi.channelMessage("Error finding existing throne holder ID. Unable to remove VIP.");
            out.println("Error finding existing throne holder ID. Unable to remove VIP.");
        } else {
            vipDb.editThroneProp(oldUserId, false);
        }
        
        // add new VIP
        if (!vipIds.contains(newUserId)) {
            try {
                twitchApi.vipAdd(newUserId);
                System.out.println("VIP added to user " + newUserId);
            } catch (HystrixRuntimeException e) {
                twitchApi.channelMessage("API error adding new VIP");
                out.printf("API error adding %s (%s) as VIP\n", newUsername, newUserId);
            }
        }
        
        // remove old VIP
        if (oldUserId != null && !vipDb.hasVip(oldUserId)) {
            try {
                twitchApi.vipRemove(oldUserId);
                System.out.println("VIP removed from user " + newUserId);
            } catch (HystrixRuntimeException e) {
                twitchApi.channelMessage("API error removing old VIP");
                out.printf("API error removing %s (%s) as VIP\n", oldUsername, oldUserId);
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
