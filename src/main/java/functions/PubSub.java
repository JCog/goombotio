package functions;

import com.github.twitch4j.eventsub.domain.RedemptionStatus;
import com.github.twitch4j.eventsub.events.ChannelPredictionEvent;
import com.github.twitch4j.helix.domain.CustomReward;
import com.github.twitch4j.helix.domain.User;
import com.github.twitch4j.pubsub.domain.ChannelPointsReward;
import com.github.twitch4j.pubsub.events.*;
import com.netflix.hystrix.exception.HystrixRuntimeException;
import database.DbManager;
import database.misc.BitWarDb;
import org.apache.commons.lang.SystemUtils;
import util.FileWriter;
import util.TwitchApi;
import util.TwitchPubSubClient;

import java.util.Arrays;
import java.util.Collections;

import static java.lang.System.out;

public class PubSub extends TwitchPubSubClient {
    private static final String[] SAVE_KEYWORDS = new String[]{"save", "love"};
    private static final String[] KILL_KEYWORDS = new String[]{"kill", "stab"};
    private static final String BIT_WAR_NAME = "save_kill_yoshi";
    private static final String TEAM_KILL = "team_kill";
    private static final String TEAM_SAVE = "team_save";
    private static final String LOCAL_RECENT_CHEER_FILENAME = "recent_cheer.txt";
    
    private static final String DETHRONE_REWARD_TITLE = "Dethrone";
    private static final String DETHRONE_REWARD_PROMPT = " currently sits on the throne. Redeem this to take their spot and increase the cost for the next person!";
    
    
    private final TwitchApi twitchApi;
    private final BitWarDb bitWarDb;
    private final String streamerId;
    
    public PubSub(TwitchApi twitchApi, DbManager dbManager, String streamerId, String authToken) {
        super(streamerId, authToken);
        this.twitchApi = twitchApi;
        bitWarDb = dbManager.getBitWarDb();
        this.streamerId = streamerId;
    }
    
    @Override
    public void onBitsEvent(ChannelBitsEvent event) {
        //updateBitWar(event);
        event.getData().getBitsUsed();
    
        String userId = event.getData().getUserId();
        User user;
        try {
            user = twitchApi.getUserById(userId);
        }
        catch (HystrixRuntimeException e) {
            e.printStackTrace();
            out.printf(
                    "error retrieving data for bit user with id %s%n",
                    userId
            );
            user = null;
        }
        if (user == null) {
            out.println("error: bit user is null");
            return;
        }
        
        String latestCheerText = String.format("%s - %d", user.getDisplayName(), event.getData().getBitsUsed());
        FileWriter.writeToFile(getOutputLocation(), LOCAL_RECENT_CHEER_FILENAME, latestCheerText);
    }
    
    @Override
    public void onChannelPointsEvent(RewardRedeemedEvent event) {
        ChannelPointsReward channelPointsReward = event.getRedemption().getReward();
        if (channelPointsReward.getTitle().startsWith(DETHRONE_REWARD_TITLE)) {
            handleDethroneReward(event);
        }
    }
    
    @Override
    public void onSubGiftsEvent(ChannelSubGiftEvent event) {
    }
    
    @Override
    public void onPredictionsEvent(ChannelPredictionEvent event) {
    
    }
    
    @Override
    public void onHypeTrainStartEvent(HypeTrainStartEvent event) {
    
    }
    
    @Override
    public void onHypeTrainEndEvent(HypeTrainEndEvent event) {
    
    }
    
    private void updateBitWar(ChannelBitsEvent event) {
        String messageText = event.getData().getChatMessage().toLowerCase();
        int bitAmount = event.getData().getBitsUsed();
        if (stringContainsItemFromList(messageText, SAVE_KEYWORDS)) {
            bitWarDb.addBits(BIT_WAR_NAME, TEAM_SAVE, bitAmount);
            twitchApi.channelMessage(String.format(
                    "%d bits have been put toward saving Yoshi jcogLove",
                    bitAmount
            ));
        }
        else if (stringContainsItemFromList(messageText, KILL_KEYWORDS)) {
            bitWarDb.addBits(BIT_WAR_NAME, TEAM_KILL, bitAmount);
            twitchApi.channelMessage(String.format(
                    "%d bits have been put toward killing Yoshi jcogBan",
                    bitAmount
            ));
        }
    }
    
    private String getOutputLocation() {
        if (SystemUtils.IS_OS_LINUX) {
            return "/srv/goombotio/";
        }
        return "output/";
    }
    
    public static boolean stringContainsItemFromList(String inputStr, String[] items) {
        return Arrays.stream(items).anyMatch(inputStr::contains);
    }
    
    private void handleDethroneReward(RewardRedeemedEvent event) {
        ChannelPointsReward channelPointsReward = event.getRedemption().getReward();
        CustomReward customReward;
        try {
            customReward = twitchApi.getCustomRewards(
                    streamerId,
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
            twitchApi.updateCustomReward(streamerId, channelPointsReward.getId(), newReward);
            success = true;
        } catch (HystrixRuntimeException e) {
            success = false;
            out.println("Error updating Dethrone reward. Refunding points.");
            twitchApi.channelMessage("Error updating Dethrone reward. Refunding points.");
        }
        
        try {
            twitchApi.updateRedemptionStatus(
                    streamerId,
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
