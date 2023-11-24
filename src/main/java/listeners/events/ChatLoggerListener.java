package listeners.events;

import com.github.twitch4j.chat.events.channel.ChannelMessageActionEvent;
import com.github.twitch4j.chat.events.channel.ChannelMessageEvent;
import com.github.twitch4j.chat.events.channel.ModAnnouncementEvent;
import listeners.TwitchEventListener;
import util.ChatLogger;

public class ChatLoggerListener implements TwitchEventListener {
    private final ChatLogger chatLogger;

    public ChatLoggerListener(ChatLogger chatLogger) {
        this.chatLogger = chatLogger;
    }

    @Override
    public void onChannelMessage(ChannelMessageEvent messageEvent) {
        chatLogger.logMessage(messageEvent);
    }
    
    @Override
    public void onAnnouncement(ModAnnouncementEvent announcementEvent) {
        chatLogger.logAnnouncement(announcementEvent);
    }
    
    @Override
    public void onChannelMessageAction(ChannelMessageActionEvent messageActionEvent) {
        chatLogger.logMessage(
                messageActionEvent.getUser().getId(),
                messageActionEvent.getUser().getName(),
                "/me " + messageActionEvent.getMessage()
        );
    }
}
