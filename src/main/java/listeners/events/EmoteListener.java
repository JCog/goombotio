package listeners.events;

import api.ThirdPartyEmoteApi;
import com.gikk.twirk.events.TwirkListener;
import com.gikk.twirk.types.emote.Emote;
import com.gikk.twirk.types.twitchMessage.TwitchMessage;
import com.gikk.twirk.types.users.TwitchUser;
import database.DbManager;
import database.emotes.BttvEmoteStatsDb;
import database.emotes.EmoteStatsDb;
import database.emotes.FfzEmoteStatsDb;
import database.emotes.SevenTvEmoteStatsDb;

import java.util.HashMap;

public class EmoteListener implements TwirkListener {
    private final EmoteStatsDb emoteStatsDb;
    private final FfzEmoteStatsDb ffzEmoteStatsDb;
    private final SevenTvEmoteStatsDb sevenTvEmoteStatsDb;
    private final BttvEmoteStatsDb bttvEmoteStatsDb;
    private final HashMap<String, String> ffzEmotes;
    private final HashMap<String, String> sevenTvEmotes;
    private final HashMap<String, String> bttvEmotes;

    public EmoteListener(DbManager dbManager) {
        emoteStatsDb = dbManager.getEmoteStatsDb();
        ffzEmoteStatsDb = dbManager.getFfzEmoteStatsDb();
        sevenTvEmoteStatsDb = dbManager.getSevenTvEmoteStatsDb();
        bttvEmoteStatsDb = dbManager.getBttvEmoteStatsDb();
        ffzEmotes = ThirdPartyEmoteApi.getFfzEmotes();
        sevenTvEmotes = ThirdPartyEmoteApi.get7tvEmotes();
        bttvEmotes = ThirdPartyEmoteApi.getBttvEmotes();
    }

    @Override
    public void onPrivMsg(TwitchUser sender, TwitchMessage message) {
        for (Emote emote : message.getEmotes()) {
            emoteStatsDb.addEmoteUsage(emote, sender.getUserID());
        }
        String[] words = message.getContent().split(" ");
        for (String word : words) {
            if (ffzEmotes.containsKey(word)) {
                ffzEmoteStatsDb.addEmoteUsage(ffzEmotes.get(word), word, sender.getUserID());
            }
            else if (sevenTvEmotes.containsKey(word)) {
                sevenTvEmoteStatsDb.addEmoteUsage(sevenTvEmotes.get(word), word, sender.getUserID());
            }
            else if (bttvEmotes.containsKey(word)) {
                bttvEmoteStatsDb.addEmoteUsage(bttvEmotes.get(word), word, sender.getUserID());
            }
        }
    }
}