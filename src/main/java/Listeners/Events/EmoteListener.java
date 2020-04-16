package Listeners.Events;
import APIs.FfzBttvApi;
import Util.Database.BttvEmoteStatsDb;
import Util.Database.EmoteStatsDb;
import Util.Database.FfzEmoteStatsDb;
import com.gikk.twirk.events.TwirkListener;
import com.gikk.twirk.types.emote.Emote;
import com.gikk.twirk.types.twitchMessage.TwitchMessage;
import com.gikk.twirk.types.users.TwitchUser;

import java.util.HashMap;

public class EmoteListener implements TwirkListener {
    private final EmoteStatsDb emoteStatsDb;
    private final FfzEmoteStatsDb ffzEmoteStatsDb;
    private final BttvEmoteStatsDb bttvEmoteStatsDb;
    private final HashMap<String, String> ffzEmotes;
    private final HashMap<String, String> bttvEmotes;
    
    public EmoteListener() {
        emoteStatsDb = EmoteStatsDb.getInstance();
        ffzEmoteStatsDb = FfzEmoteStatsDb.getInstance();
        bttvEmoteStatsDb = BttvEmoteStatsDb.getInstance();
        ffzEmotes = FfzBttvApi.getFfzEmotes();
        bttvEmotes = FfzBttvApi.getBttvEmotes();
    }
    
    @Override
    public void onPrivMsg(TwitchUser sender, TwitchMessage message) {
        for(Emote emote : message.getEmotes()) {
            emoteStatsDb.addEmoteUsage(emote, sender.getUserID());
        }
        String[] words = message.getContent().split(" ");
        for(String word : words) {
            if (ffzEmotes.containsKey(word)) {
                ffzEmoteStatsDb.addEmoteUsage(ffzEmotes.get(word), word, sender.getUserID());
            }
            else if (bttvEmotes.containsKey(word)) {
                bttvEmoteStatsDb.addEmoteUsage(bttvEmotes.get(word), word, sender.getUserID());
            }
        }
    }
}