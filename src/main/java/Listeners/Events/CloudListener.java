package Listeners.Events;

import Util.TwirkInterface;
import com.gikk.twirk.events.TwirkListener;
import com.gikk.twirk.types.twitchMessage.TwitchMessage;
import com.gikk.twirk.types.users.TwitchUser;

public class CloudListener implements TwirkListener {
    private static final String MESSAGE = "hi cloud";
    private static final long CLOUD_ID = 51671037;
    
    private final TwirkInterface twirk;
    
    private boolean saidHi;
    
    public CloudListener(TwirkInterface twirk) {
        this.twirk = twirk;
        saidHi = true;
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
