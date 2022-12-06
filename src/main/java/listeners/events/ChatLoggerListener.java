package listeners.events;

import com.github.twitch4j.chat.events.channel.ChannelMessageEvent;
import listeners.TwitchEventListener;
import util.ChatLogger;

public class ChatLoggerListener implements TwitchEventListener {
    private final ChatLogger chatLogger;

    public ChatLoggerListener(ChatLogger chatLogger) {
        this.chatLogger = chatLogger;
    }

    @Override
    public void onPrivMsg(ChannelMessageEvent messageEvent) {
        chatLogger.logMessage(messageEvent);
    }


}
