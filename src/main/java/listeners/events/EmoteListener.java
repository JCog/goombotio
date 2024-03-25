package listeners.events;

import api.ThirdPartyEmoteApi;
import com.github.twitch4j.chat.events.channel.ChannelMessageEvent;
import database.emotes.BttvEmoteStatsDb;
import database.emotes.FfzEmoteStatsDb;
import database.emotes.SevenTvEmoteStatsDb;
import listeners.TwitchEventListener;
import util.CommonUtils;

import java.util.Map;

public class EmoteListener implements TwitchEventListener {
//    private final EmoteStatsDb emoteStatsDb;
    private final FfzEmoteStatsDb ffzEmoteStatsDb;
    private final SevenTvEmoteStatsDb sevenTvEmoteStatsDb;
    private final BttvEmoteStatsDb bttvEmoteStatsDb;
    private final Map<String, String> ffzEmotes;
    private final Map<String, String> sevenTvEmotes;
    private final Map<String, String> bttvEmotes;

    public EmoteListener(CommonUtils commonUtils) {
//        emoteStatsDb = commonUtils.getDbManager().getEmoteStatsDb();
        ffzEmoteStatsDb = commonUtils.getDbManager().getFfzEmoteStatsDb();
        sevenTvEmoteStatsDb = commonUtils.getDbManager().getSevenTvEmoteStatsDb();
        bttvEmoteStatsDb = commonUtils.getDbManager().getBttvEmoteStatsDb();
        
        String username = commonUtils.getTwitchApi().getStreamerUser().getLogin();
        ffzEmotes = commonUtils.getApiManager().getFfzApi().getEmotes(username);
        sevenTvEmotes = ThirdPartyEmoteApi.get7tvEmotes();
        bttvEmotes = ThirdPartyEmoteApi.getBttvEmotes();
    }

    @Override
    public void onChannelMessage(ChannelMessageEvent messageEvent) {
        // Twitch4J doesn't supply data on what Twitch emotes are in a message, so can't easily track this anymore
//        for (Emote emote : message.getEmotes()) {
//            emoteStatsDb.addEmoteUsage(emote, sender.getUserID());
//        }
        String[] words = messageEvent.getMessage().split(" ");
        long userId = Long.parseLong(messageEvent.getUser().getId());
        for (String word : words) {
            if (ffzEmotes.containsKey(word)) {
                ffzEmoteStatsDb.addEmoteUsage(ffzEmotes.get(word), word, userId);
            } else if (sevenTvEmotes.containsKey(word)) {
                sevenTvEmoteStatsDb.addEmoteUsage(sevenTvEmotes.get(word), word, userId);
            } else if (bttvEmotes.containsKey(word)) {
                bttvEmoteStatsDb.addEmoteUsage(bttvEmotes.get(word), word, userId);
            }
        }
    }
}