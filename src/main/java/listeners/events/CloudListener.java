package listeners.events;

import com.github.twitch4j.chat.events.channel.ChannelMessageEvent;
import com.github.twitch4j.events.ChannelGoLiveEvent;
import listeners.TwitchEventListener;
import util.TwitchApi;

import java.util.Objects;

public class CloudListener implements TwitchEventListener {
    private static final String MESSAGE = "hi cloud";
    private static final String CLOUD_ID = "51671037";

    private final TwitchApi twitchApi;

    private boolean saidHi;

    public CloudListener(TwitchApi twitchApi) {
        this.twitchApi = twitchApi;
        saidHi = true;
    }

    @Override
    public void onPrivMsg(ChannelMessageEvent messageEvent) {
        if (Objects.equals(messageEvent.getUser().getId(), CLOUD_ID) && !saidHi) {
            twitchApi.channelMessage(MESSAGE);
            saidHi = true;
        }
    }
    
    @Override
    public void onGoLive(ChannelGoLiveEvent goLiveEvent) {
        saidHi = false;
    }
}
