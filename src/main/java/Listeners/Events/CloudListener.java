package Listeners.Events;

import com.gikk.twirk.Twirk;
import com.gikk.twirk.events.TwirkListener;
import com.gikk.twirk.types.twitchMessage.TwitchMessage;
import com.gikk.twirk.types.users.TwitchUser;

public class CloudListener implements TwirkListener {
    private static final String MESSAGE = "hi cloud";
    private static final long CLOUD_ID = 51671037;
    
    private final Twirk twirk;
    
    private boolean saidHi;
    
    public CloudListener(Twirk twirk) {
        this.twirk = twirk;
        saidHi = false;
    }
    
    @Override
    public void onPrivMsg(TwitchUser sender, TwitchMessage message) {
        if (sender.getUserID() == CLOUD_ID && !saidHi) {
            twirk.channelMessage(MESSAGE);
            saidHi = true;
        }
    }
    
    public void reset() {
        saidHi = false;
    }
}