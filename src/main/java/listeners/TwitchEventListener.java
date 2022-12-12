package listeners;

import com.github.twitch4j.chat.events.channel.ChannelMessageEvent;
import com.github.twitch4j.events.ChannelChangeGameEvent;
import com.github.twitch4j.events.ChannelChangeTitleEvent;
import com.github.twitch4j.events.ChannelGoLiveEvent;
import com.github.twitch4j.events.ChannelGoOfflineEvent;
import com.github.twitch4j.pubsub.events.ChannelBitsEvent;
import com.github.twitch4j.pubsub.events.ChannelSubGiftEvent;
import com.github.twitch4j.pubsub.events.ChannelSubscribeEvent;
import com.github.twitch4j.pubsub.events.RewardRedeemedEvent;

public interface TwitchEventListener {
    ////////////////// Events //////////////////
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
