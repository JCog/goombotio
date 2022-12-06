package listeners;

import com.github.twitch4j.chat.events.channel.ChannelMessageEvent;
import com.github.twitch4j.events.ChannelGoLiveEvent;
import com.github.twitch4j.pubsub.events.ChannelBitsEvent;
import com.github.twitch4j.pubsub.events.ChannelSubGiftEvent;
import com.github.twitch4j.pubsub.events.ChannelSubscribeEvent;
import com.github.twitch4j.pubsub.events.RewardRedeemedEvent;

public interface TwitchEventListener {
    ////////////////// Events //////////////////
    default void onPrivMsg(ChannelMessageEvent messageEvent) {}
    
    default void onGoLive(ChannelGoLiveEvent goLiveEvent) {}
    
    ////////////////// PubSub //////////////////
    default void onBits(ChannelBitsEvent bitsEvent) {}
    
    default void onChannelPointsRedemption(RewardRedeemedEvent channelPointsEvent) {}
    
    default void onSub(ChannelSubscribeEvent subEvent) {}
    
    default void onSubGift(ChannelSubGiftEvent subGiftEvent) {}
}
