package listeners.events;

import api.FfzBttvApi;
import com.gikk.twirk.events.TwirkListener;
import com.gikk.twirk.types.emote.Emote;
import com.gikk.twirk.types.twitchMessage.TwitchMessage;
import com.gikk.twirk.types.users.TwitchUser;
import database.DbManager;
import database.emotes.BttvEmoteStatsDb;
import database.emotes.EmoteStatsDb;
import database.emotes.FfzEmoteStatsDb;

import java.util.HashMap;

public class EmoteListener implements TwirkListener {
    private final EmoteStatsDb emoteStatsDb;
    private final FfzEmoteStatsDb ffzEmoteStatsDb;
    private final BttvEmoteStatsDb bttvEmoteStatsDb;
    private final HashMap<String, String> ffzEmotes;
    private final HashMap<String, String> bttvEmotes;

    public EmoteListener(DbManager dbManager) {
        emoteStatsDb = dbManager.getEmoteStatsDb();
        ffzEmoteStatsDb = dbManager.getFfzEmoteStatsDb();
        bttvEmoteStatsDb = dbManager.getBttvEmoteStatsDb();
        ffzEmotes = FfzBttvApi.getFfzEmotes();
        bttvEmotes = FfzBttvApi.getBttvEmotes();
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
            else if (bttvEmotes.containsKey(word)) {
                bttvEmoteStatsDb.addEmoteUsage(bttvEmotes.get(word), word, sender.getUserID());
            }
        }
    }
}