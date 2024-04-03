package listeners.events;

import com.github.twitch4j.chat.events.channel.SubscriptionEvent;
import com.github.twitch4j.common.enums.SubscriptionPlan;
import com.github.twitch4j.eventsub.events.ChannelSubscriptionGiftEvent;
import com.github.twitch4j.eventsub.events.ChannelSubscriptionMessageEvent;
import listeners.TwitchEventListener;
import org.apache.commons.lang.SystemUtils;
import util.CommonUtils;
import util.FileWriter;
import util.TwitchApi;

public class SubListener implements TwitchEventListener {
    
    private static final String LOCAL_RECENT_SUB_FILENAME = "recent_sub.txt";
    
    private final TwitchApi twitchApi;

    public SubListener(CommonUtils commonUtils) {
        twitchApi = commonUtils.getTwitchApi();
    }
    
    @Override
    public void onSubscribe(SubscriptionEvent subEvent) {
        if (subEvent.getGifted()) {
            return;
        }
        String displayName = TwitchEventListener.getDisplayName(subEvent.getMessageEvent());
        String tier = getSubType(subEvent.getSubPlan());
        if (subEvent.getMonths() == 1) {
            twitchApi.channelMessage(String.format(
                    "jcogChamp @%s Thank you so much for the %s sub! Welcome to the Rookery™! jcogChamp",
                    displayName,
                    tier
            ));
        } else {
            twitchApi.channelMessage(String.format(
                    "jcogChamp @%s Thank you so much for the %d-month %s resub! Welcome back to the Rookery™! jcogChamp",
                    displayName,
                    subEvent.getMonths(),
                    tier
            ));
        }
        outputRecentSubFile(displayName);
    }
    
    @Override
    public void onResubscribe(ChannelSubscriptionMessageEvent resubEvent) {
        // reimplement this if Twitch ever gets it together with EventSub
//        twitchApi.channelMessage(String.format(
//                "jcogChamp @%s Thank you so much for the %d-month %s resub! Welcome back to the Rookery™! jcogChamp",
//                resubEvent.getUserName(),
//                resubEvent.getCumulativeMonths(),
//                getSubType(resubEvent.getTier())
//        ));
//        outputRecentSubFile(resubEvent.getUserName());
    }
    
    @Override
    public void onSubGift(ChannelSubscriptionGiftEvent subGiftEvent) {
        String username = subGiftEvent.getUserName();
        int count = subGiftEvent.getTotal();
        String tier = getSubType(subGiftEvent.getTier());
        
        if (count == 1) {
            twitchApi.channelMessage(String.format(
                    "jcogChamp @%s Thank you so much for the %s gift sub! jcogChamp",
                    username,
                    tier
            ));
        } else {
            twitchApi.channelMessage(String.format(
                    "jcogChamp @%s Thank you so much for the %d %s gift subs! jcogChamp",
                    username,
                    count,
                    tier
            ));
        }
    }
    
    private String getSubType(SubscriptionPlan subPlan) {
        switch (subPlan) {
            case TIER1: return "Tier 1";
            case TIER2: return "Tier 2";
            case TIER3: return "Tier 3";
            case TWITCH_PRIME: return "Prime Gaming";
            default: return "";
        }
    }
    
    private void outputRecentSubFile(String displayName) {
        FileWriter.writeToFile(getOutputLocation(), LOCAL_RECENT_SUB_FILENAME, displayName);
    }
    
    private String getOutputLocation() {
        if (SystemUtils.IS_OS_LINUX) {
            return "/srv/goombotio/";
        }
        return "output/";
    }

}
