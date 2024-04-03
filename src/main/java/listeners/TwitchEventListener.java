package listeners;

import com.github.twitch4j.chat.events.channel.*;
import com.github.twitch4j.eventsub.events.*;

import java.util.*;

public interface TwitchEventListener {
    static String getDisplayName(IRCMessageEvent messageEvent) {
        if (messageEvent.getUserDisplayName().isPresent()) {
            return messageEvent.getUserDisplayName().get();
        } else {
            return messageEvent.getUserName();
        }
    }
    
    static List<Map.Entry<String, Integer>> getEmoteUsageCounts(ChannelMessageEvent messageEvent) {
        Optional<String> emotesTag = messageEvent.getMessageEvent().getTagValue("emotes");
        if (emotesTag.isEmpty()) {
            return new ArrayList<>();
        }
    
        List<Map.Entry<String, Integer>> emoteUsages = new ArrayList<>();
        String[] emotes = emotesTag.get().split("/");
        for (String emote : emotes) {
            String id = emote.split(":", 2)[0];
            int usages = emote.split(",").length;
            emoteUsages.add(new AbstractMap.SimpleEntry<>(id, usages));
        }
        return emoteUsages;
    }
    
    ////////////////// EventSub //////////////////
    default void onAdBegin(ChannelAdBreakBeginEvent adEvent) {}
    default void onRaid(ChannelRaidEvent raidEvent) {}
    default void onGoLive(StreamOnlineEvent goLiveEvent) {}
    default void onGoOffline(StreamOfflineEvent goOfflineEvent) {}
    default void onChannelUpdate(ChannelUpdateV2Event updateEvent) {}
    default void onChannelPointsRedemption(CustomRewardRedemptionAddEvent channelPointsEvent) {}
    default void onCheer(ChannelCheerEvent cheerEvent) {}
    default void onSubscribe(SubscriptionEvent subEvent) {} // doesn't include resubs
    default void onResubscribe(ChannelSubscriptionMessageEvent resubEvent) {}
    default void onSubGift(ChannelSubscriptionGiftEvent subGiftEvent) {}
    
    ////////////////// Chat //////////////////
    default void onAnnouncement(ModAnnouncementEvent announcementEvent) {}
    default void onChannelMessageAction(ChannelMessageActionEvent messageActionEvent) {}
    default void onChannelMessage(ChannelMessageEvent messageEvent) {}
}
