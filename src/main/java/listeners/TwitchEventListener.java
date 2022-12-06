package listeners;

import com.github.twitch4j.chat.events.channel.ChannelMessageEvent;
import com.github.twitch4j.events.ChannelGoLiveEvent;

public interface TwitchEventListener {
    default void onPrivMsg(ChannelMessageEvent messageEvent) {}
    
    default void onGoLive(ChannelGoLiveEvent goLiveEvent) {}
}
