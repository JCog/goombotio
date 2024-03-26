package listeners;

import com.github.twitch4j.chat.events.channel.ChannelMessageActionEvent;
import com.github.twitch4j.chat.events.channel.ChannelMessageEvent;
import com.github.twitch4j.chat.events.channel.ModAnnouncementEvent;
import com.github.twitch4j.eventsub.events.*;

import java.util.*;

public interface TwitchEventListener {
    static String getDisplayName(ChannelMessageEvent messageEvent) {
        if (messageEvent.getMessageEvent().getUserDisplayName().isPresent()) {
            return messageEvent.getMessageEvent().getUserDisplayName().get();
        } else {
            return messageEvent.getUser().getName();
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
    default void onSubscribe(ChannelSubscribeEvent subEvent) {} // doesn't include resubs
    default void onResubscribe(ChannelSubscriptionMessageEvent resubEvent) {}
    default void onSubGift(ChannelSubscriptionGiftEvent subGiftEvent) {}
    
    ////////////////// Chat //////////////////
    default void onAnnouncement(ModAnnouncementEvent announcementEvent) {}
    default void onChannelMessageAction(ChannelMessageActionEvent messageActionEvent) {}
    default void onChannelMessage(ChannelMessageEvent messageEvent) {}
}
