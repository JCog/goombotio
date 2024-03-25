package listeners;

import com.github.twitch4j.chat.events.channel.ChannelMessageActionEvent;
import com.github.twitch4j.chat.events.channel.ChannelMessageEvent;
import com.github.twitch4j.chat.events.channel.ModAnnouncementEvent;
import com.github.twitch4j.events.ChannelChangeGameEvent;
import com.github.twitch4j.events.ChannelChangeTitleEvent;
import com.github.twitch4j.events.ChannelGoLiveEvent;
import com.github.twitch4j.events.ChannelGoOfflineEvent;
import com.github.twitch4j.eventsub.events.ChannelRaidEvent;
import com.github.twitch4j.pubsub.events.*;

public interface TwitchEventListener {
    static String getDisplayName(ChannelMessageEvent messageEvent) {
        if (messageEvent.getMessageEvent().getUserDisplayName().isPresent()) {
            return messageEvent.getMessageEvent().getUserDisplayName().get();
        } else {
            return messageEvent.getUser().getName();
        }
    }
    
    ////////////////// Events //////////////////
    default void onMidrollRequest(MidrollRequestEvent adsEvent) {}
    
    default void onAnnouncement(ModAnnouncementEvent announcementEvent) {}
    
    default void onChannelMessage(ChannelMessageEvent messageEvent) {}
    
    default void onChannelMessageAction(ChannelMessageActionEvent messageActionEvent) {}
    
    default void onGoLive(ChannelGoLiveEvent goLiveEvent) {}
    
    default void onGoOffline(ChannelGoOfflineEvent goOfflineEvent) {}
    
    default void onGameChange(ChannelChangeGameEvent changeGameEvent) {}
    
    default void onChangeTitle(ChannelChangeTitleEvent changeTitleEvent) {}
    
    default void onRaid(ChannelRaidEvent raidEvent) {}
    
    ////////////////// PubSub //////////////////
    default void onCheer(ChannelBitsEvent bitsEvent) {}
    
    default void onChannelPointsRedemption(RewardRedeemedEvent channelPointsEvent) {}
    
    default void onSub(ChannelSubscribeEvent subEvent) {}
    
    default void onSubGift(ChannelSubGiftEvent subGiftEvent) {}
}
