package listeners;

import com.github.twitch4j.chat.events.channel.*;
import com.github.twitch4j.eventsub.events.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public interface TwitchEventListener {
    static String getDisplayName(IRCMessageEvent messageEvent) {
        if (messageEvent.getUserDisplayName().isPresent()) {
            return messageEvent.getUserDisplayName().get();
        } else {
            return messageEvent.getUserName();
        }
    }
    
    record EmoteUsage(String emoteId, String pattern, int usageCount) {}
    
    static List<EmoteUsage> getEmoteUsageCounts(ChannelMessageEvent messageEvent) {
        Optional<String> emotesTag = messageEvent.getMessageEvent().getTagValue("emotes");
        if (emotesTag.isEmpty()) {
            return new ArrayList<>();
        }
    
        List<EmoteUsage> emoteUsages = new ArrayList<>();
        String[] emotes = emotesTag.get().split("/");
        String message = messageEvent.getMessage();
        for (String emote : emotes) {
            String[] split = emote.split(":", 2);
            String id = split[0];
            String[] locations = split[1].split(",");
            String[] range = locations[0].split("-");
            int start = Integer.parseInt(range[0]);
            int end = Integer.parseInt(range[1]) + 1;
            String pattern = message.substring(start, end);
            int usageCount = locations.length;
            emoteUsages.add(new EmoteUsage(id, pattern, usageCount));
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
    default void onHypeTrainBegin(HypeTrainBeginEvent hypeTrainBeginEvent) {}
    
    ////////////////// Chat //////////////////
    default void onAnnouncement(ModAnnouncementEvent announcementEvent) {}
    default void onChannelMessageAction(ChannelMessageActionEvent messageActionEvent) {}
    default void onChannelMessage(ChannelMessageEvent messageEvent) {}
}
