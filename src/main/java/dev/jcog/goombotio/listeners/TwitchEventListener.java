package dev.jcog.goombotio.listeners;

import com.github.twitch4j.chat.events.channel.*;
import com.github.twitch4j.eventsub.events.*;
import dev.jcog.goombotio.util.TwitchApi;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public interface TwitchEventListener {
    enum EVENT_TYPE {
        AD_BEGIN,
        RAID,
        GO_LIVE,
        GO_OFFLINE,
        CHANNEL_UPDATE,
        CHANNEL_POINTS_REDEMPTION,
        CHEER,
        //        SUBSCRIBE,
        RESUBSCRIBE,
        SUB_GIFT,
        HYPE_TRAIN_BEGIN,

        ANNOUNCEMENT,
        CHANNEL_MESSAGE_ACTION,
        CHANNEL_MESSAGE,
        SUBSCRIBE,
    }

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

    List<EVENT_TYPE> getEventTypes();
    
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
    default void onHypeTrainBegin(HypeTrainBeginV2Event hypeTrainBeginEvent) {}
    
    ////////////////// Chat //////////////////
    default void onAnnouncement(ModAnnouncementEvent announcementEvent) {}
    default void onChannelMessageAction(ChannelMessageActionEvent messageActionEvent) {}
    default void onChannelMessage(ChannelMessageEvent messageEvent) {}
}
