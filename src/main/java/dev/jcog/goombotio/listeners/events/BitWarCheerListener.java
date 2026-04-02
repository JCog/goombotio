package dev.jcog.goombotio.listeners.events;

import com.github.twitch4j.eventsub.events.ChannelCheerEvent;
import dev.jcog.goombotio.database.misc.BitWarDb;
import dev.jcog.goombotio.listeners.TwitchEventListener;
import dev.jcog.goombotio.util.CommonUtils;
import dev.jcog.goombotio.util.TwitchApi;

import java.util.Arrays;
import java.util.List;

import static dev.jcog.goombotio.listeners.TwitchEventListener.EVENT_TYPE.CHEER;

public class BitWarCheerListener implements TwitchEventListener {
    private static final String[] SAVE_KEYWORDS = new String[]{"save", "love"};
    private static final String[] KILL_KEYWORDS = new String[]{"kill", "stab"};
    private static final String BIT_WAR_NAME = "save_kill_yoshi";
    private static final String TEAM_KILL = "team_kill";
    private static final String TEAM_SAVE = "team_save";
    
    private final TwitchApi twitchApi;
    private final BitWarDb bitWarDb;
    
    public BitWarCheerListener(CommonUtils commonUtils) {
        twitchApi = commonUtils.twitchApi();
        bitWarDb = commonUtils.dbManager().getBitWarDb();
    }

    @Override
    public List<EVENT_TYPE> getEventTypes() {
        return List.of(CHEER);
    }

    @Override
    public void onCheer(ChannelCheerEvent bitsEvent) {
        String messageText = bitsEvent.getMessage().toLowerCase();
        int bitAmount = bitsEvent.getBits();
        if (stringContainsItemFromList(messageText, SAVE_KEYWORDS)) {
            bitWarDb.addBits(BIT_WAR_NAME, TEAM_SAVE, bitAmount);
            twitchApi.channelMessage(String.format(
                    "%d bits have been put toward saving Yoshi jcogLove",
                    bitAmount
            ));
        } else if (stringContainsItemFromList(messageText, KILL_KEYWORDS)) {
            bitWarDb.addBits(BIT_WAR_NAME, TEAM_KILL, bitAmount);
            twitchApi.channelMessage(String.format(
                    "%d bits have been put toward killing Yoshi jcogBan",
                    bitAmount
            ));
        }
    }
    
    private static boolean stringContainsItemFromList(String inputStr, String[] items) {
        return Arrays.stream(items).anyMatch(inputStr::contains);
    }
}
