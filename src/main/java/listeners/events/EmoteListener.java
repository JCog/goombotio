package listeners.events;

import com.github.twitch4j.chat.events.channel.ChannelMessageEvent;
import database.emotes.BttvEmoteStatsDb;
import database.emotes.EmoteStatsDb;
import database.emotes.FfzEmoteStatsDb;
import database.emotes.SevenTvEmoteStatsDb;
import listeners.TwitchEventListener;
import util.CommonUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class EmoteListener implements TwitchEventListener {
    private final EmoteStatsDb emoteStatsDb;
    private final FfzEmoteStatsDb ffzEmoteStatsDb;
    private final SevenTvEmoteStatsDb sevenTvEmoteStatsDb;
    private final BttvEmoteStatsDb bttvEmoteStatsDb;
    private final Map<String, String> ffzEmotes;
    private final Map<String, String> sevenTvEmotes;
    private final Map<String, String> bttvEmotes;

    public EmoteListener(CommonUtils commonUtils) {
        emoteStatsDb = commonUtils.dbManager().getEmoteStatsDb();
        ffzEmoteStatsDb = commonUtils.dbManager().getFfzEmoteStatsDb();
        sevenTvEmoteStatsDb = commonUtils.dbManager().getSevenTvEmoteStatsDb();
        bttvEmoteStatsDb = commonUtils.dbManager().getBttvEmoteStatsDb();
        
        String username = commonUtils.twitchApi().getStreamerUser().getLogin();
        String userId = commonUtils.twitchApi().getStreamerUser().getId();
        ffzEmotes = getInverseMap(commonUtils.apiManager().getFfzApi().getEmotes(username));
        sevenTvEmotes = getInverseMap(commonUtils.apiManager().getSevenTvApi().getEmotes(userId));
        bttvEmotes = getInverseMap(commonUtils.apiManager().getBttvApi().getEmotes(userId));
    }

    private Map<String, String> getInverseMap(Map<String, String> map) {
        return map.entrySet().stream().collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));
    }

    @Override
    public void onChannelMessage(ChannelMessageEvent messageEvent) {
        long userId = Long.parseLong(messageEvent.getUser().getId());
        List<EmoteUsage> emoteUsages = TwitchEventListener.getEmoteUsageCounts(messageEvent);
        for (EmoteUsage emote : emoteUsages) {
            for (int i = 0; i < emote.usageCount(); i++) {
                emoteStatsDb.addEmoteUsage(emote.emoteId(), null, userId);
            }
        }
        
        String[] words = messageEvent.getMessage().split(" ");
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