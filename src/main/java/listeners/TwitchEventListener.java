package listeners;

import com.github.twitch4j.chat.events.channel.ChannelMessageEvent;
import com.github.twitch4j.events.ChannelChangeGameEvent;
import com.github.twitch4j.events.ChannelChangeTitleEvent;
import com.github.twitch4j.events.ChannelGoLiveEvent;
import com.github.twitch4j.events.ChannelGoOfflineEvent;
import com.github.twitch4j.pubsub.events.*;

import java.util.Map;

public interface TwitchEventListener {
    // TODO: should probably look into doing this in a more general way, without requiring the tags directly
    static String getDisplayName(Map<String, String> eventTags) {
        return eventTags.get("display-name");
    }
    
    ////////////////// Events //////////////////
    default void onMidrollRequest(MidrollRequestEvent adsEvent) {}
    
    default void onChannelMessage(ChannelMessageEvent messageEvent) {}
    
    default void onGoLive(ChannelGoLiveEvent goLiveEvent) {}
    
    default void onGoOffline(ChannelGoOfflineEvent goOfflineEvent) {}
    
    default void onGameChange(ChannelChangeGameEvent changeGameEvent) {}
    
    default void onChangeTitle(ChannelChangeTitleEvent changeTitleEvent) {}
    
    ////////////////// PubSub //////////////////
    default void onCheer(ChannelBitsEvent bitsEvent) {}
    
    default void onChannelPointsRedemption(RewardRedeemedEvent channelPointsEvent) {}
    
    default void onSub(ChannelSubscribeEvent subEvent) {}
    
    default void onSubGift(ChannelSubGiftEvent subGiftEvent) {}
}
