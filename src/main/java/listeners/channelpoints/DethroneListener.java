package listeners.channelpoints;

import com.github.twitch4j.eventsub.domain.RedemptionStatus;
import com.github.twitch4j.helix.domain.CustomReward;
import com.github.twitch4j.pubsub.domain.ChannelPointsReward;
import com.github.twitch4j.pubsub.events.RewardRedeemedEvent;
import com.netflix.hystrix.exception.HystrixRuntimeException;
import listeners.TwitchEventListener;
import util.TwitchApi;

import java.util.Collections;

import static java.lang.System.out;

public class DethroneListener implements TwitchEventListener {
    private static final String DETHRONE_REWARD_TITLE = "Dethrone";
    private static final String DETHRONE_REWARD_PROMPT = " currently sits on the throne. Redeem this to take their spot and increase the cost for the next person!";
    
    private final TwitchApi twitchApi;
    
    public DethroneListener(TwitchApi twitchApi) {
        this.twitchApi = twitchApi;
    }
    
    @Override
    public void onChannelPointsRedemption(RewardRedeemedEvent event) {
        ChannelPointsReward channelPointsReward = event.getRedemption().getReward();
        if (channelPointsReward.getTitle().startsWith(DETHRONE_REWARD_TITLE)) {
            handleDethroneReward(event);
        }
    }
    
    
    private void handleDethroneReward(RewardRedeemedEvent event) {
        ChannelPointsReward channelPointsReward = event.getRedemption().getReward();
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
        
        String oldUser = channelPointsReward.getTitle().split("\\s")[1];
        String newUser = event.getRedemption().getUser().getDisplayName();
        int newCost = getNextIncreasedCost(customReward.getCost());
        CustomReward newReward = customReward
                .withCost(newCost)
                .withTitle(DETHRONE_REWARD_TITLE + " " + newUser)
                .withPrompt(newUser + DETHRONE_REWARD_PROMPT);
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
                    Collections.singletonList(event.getRedemption().getId()),
                    success ? RedemptionStatus.FULFILLED : RedemptionStatus.CANCELED
            );
        } catch (HystrixRuntimeException e) {
            twitchApi.channelMessage(String.format("@JCog error %s Dethrone reward. Please do so manually while shaking your fist at twitch.", success ? "fulfilling" : "refunding"));
        }
        if (success) {
            twitchApi.channelMessage(String.format("%s has taken the throne from %s! The cost to dethrone them has increased to %d. jcogBan", newUser, oldUser, newCost));
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
