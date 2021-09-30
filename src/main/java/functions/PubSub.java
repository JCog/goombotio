package functions;

import com.github.twitch4j.eventsub.events.ChannelPredictionEvent;
import com.github.twitch4j.pubsub.events.*;
import database.DbManager;
import database.misc.BitWarDb;
import util.TwirkInterface;
import util.TwitchPubSubClient;

import java.util.Arrays;

public class PubSub extends TwitchPubSubClient {
    private static final String[] SAVE_KEYWORDS = new String[]{"save", "love"};
    private static final String[] KILL_KEYWORDS = new String[]{"kill", "stab"};
    private static final String BIT_WAR_NAME = "save_kill_yoshi";
    private static final String TEAM_KILL = "team_kill";
    private static final String TEAM_SAVE = "team_save";
    
    private final TwirkInterface twirk;
    private final BitWarDb bitWarDb;
    
    public PubSub(TwirkInterface twirk, DbManager dbManager, String streamerId, String authToken) {
        super(streamerId, authToken);
        this.twirk = twirk;
        bitWarDb = dbManager.getBitWarDb();
    }
    
    @Override
    public void onBitsEvent(ChannelBitsEvent event) {
        updateBitWar(event);
    }
    
    @Override
    public void onChannelPointsEvent(RewardRedeemedEvent event) {
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
            twirk.channelMessage(String.format(
                    "%d bits have been put toward saving Yoshi jcogLove",
                    bitAmount
            ));
        }
        else if (stringContainsItemFromList(messageText, KILL_KEYWORDS)) {
            bitWarDb.addBits(BIT_WAR_NAME, TEAM_KILL, bitAmount);
            twirk.channelMessage(String.format(
                    "%d bits have been put toward killing Yoshi jcogBan",
                    bitAmount
            ));
        }
    }
    
    public static boolean stringContainsItemFromList(String inputStr, String[] items) {
        return Arrays.stream(items).anyMatch(inputStr::contains);
    }
}
