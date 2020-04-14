package Listeners.Events;
import Util.Database.EmoteStatsDb;
import com.gikk.twirk.events.TwirkListener;
import com.gikk.twirk.types.emote.Emote;
import com.gikk.twirk.types.twitchMessage.TwitchMessage;
import com.gikk.twirk.types.users.TwitchUser;

public class EmoteListener implements TwirkListener {
    private EmoteStatsDb emoteStatsDb;
    
    public EmoteListener() {
        emoteStatsDb = EmoteStatsDb.getInstance();
    }
    
    @Override
    public void onPrivMsg(TwitchUser sender, TwitchMessage message) {
        for(Emote emote : message.getEmotes()) {
            emoteStatsDb.addEmoteUsage(emote, sender.getUserID());
        }
    }
}