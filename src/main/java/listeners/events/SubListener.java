package listeners.events;

import com.github.twitch4j.pubsub.domain.SubGiftData;
import com.github.twitch4j.pubsub.domain.SubscriptionData;
import com.github.twitch4j.pubsub.events.ChannelSubGiftEvent;
import com.github.twitch4j.pubsub.events.ChannelSubscribeEvent;
import listeners.TwitchEventListener;
import org.apache.commons.lang.SystemUtils;
import util.FileWriter;
import util.TwitchApi;

public class SubListener implements TwitchEventListener {
    
    private static final String LOCAL_RECENT_SUB_FILENAME = "recent_sub.txt";
    
    private final TwitchApi twitchApi;

    public SubListener(TwitchApi twitchApi) {
        this.twitchApi = twitchApi;
    }
    
    @Override
    public void onSubGift(ChannelSubGiftEvent subGiftEvent) {
        SubGiftData subData = subGiftEvent.getData();
        String username = subData.getDisplayName();
        int count = subData.getCount();
        String tier;
        switch (subData.getTier()) {
            case TIER1: tier = "Tier 1"; break;
            case TIER2: tier = "Tier 2"; break;
            case TIER3: tier = "Tier 3"; break;
            default: tier = "";
        }
    
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
    
    @Override
    public void onSub(ChannelSubscribeEvent subEvent) {
        SubscriptionData subData = subEvent.getData();
        if (!subData.getIsGift()) {
            String username = subData.getDisplayName();
            int months = subData.getCumulativeMonths();
            String type;
            switch (subData.getSubPlan()) {
                case TIER1: type = "Tier 1"; break;
                case TIER2: type = "Tier 2"; break;
                case TIER3: type = "Tier 3"; break;
                case TWITCH_PRIME: type = "Prime Gaming"; break;
                default: type = "";
            }
            
            if (months == 1) {
                twitchApi.channelMessage(String.format(
                        "jcogChamp @%s Thank you so much for the %s sub! Welcome to the Rookery™! jcogChamp",
                        username,
                        type
                ));
            } else {
                twitchApi.channelMessage(String.format(
                        "jcogChamp @%s Thank you so much for the %d-month %s resub! Welcome back to the Rookery™! jcogChamp",
                        username,
                        months,
                        type
                ));
            }
            
            outputRecentSubFile(username);
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
