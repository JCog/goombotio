package listeners.events;

import com.gikk.twirk.events.TwirkListener;
import com.gikk.twirk.types.twitchMessage.TwitchMessage;
import com.gikk.twirk.types.users.TwitchUser;
import util.TwitchApi;

public class CloudListener implements TwirkListener {
    private static final String MESSAGE = "hi cloud";
    private static final long CLOUD_ID = 51671037;

    private final TwitchApi twitchApi;

    private boolean saidHi;

    public CloudListener(TwitchApi twitchApi) {
        this.twitchApi = twitchApi;
        saidHi = true;
    }

    @Override
    public void onPrivMsg(TwitchUser sender, TwitchMessage message) {
        if (sender.getUserID() == CLOUD_ID && !saidHi) {
            twitchApi.channelMessage(MESSAGE);
            saidHi = true;
        }
    }

    public void reset() {
        saidHi = false;
    }
}
