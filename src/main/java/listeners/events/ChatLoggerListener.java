package listeners.events;

import com.github.twitch4j.chat.events.channel.ChannelMessageActionEvent;
import com.github.twitch4j.chat.events.channel.ChannelMessageEvent;
import com.github.twitch4j.chat.events.channel.ModAnnouncementEvent;
import listeners.TwitchEventListener;
import util.ChatLogger;

public class ChatLoggerListener implements TwitchEventListener {
    @Override
    public void onChannelMessage(ChannelMessageEvent messageEvent) {
        ChatLogger.logMessage(messageEvent);
    }
    
    @Override
    public void onAnnouncement(ModAnnouncementEvent announcementEvent) {
        ChatLogger.logAnnouncement(announcementEvent);
    }
    
    @Override
    public void onChannelMessageAction(ChannelMessageActionEvent messageActionEvent) {
        ChatLogger.logMessage(
                messageActionEvent.getUser().getId(),
                messageActionEvent.getUser().getName(),
                "/me " + messageActionEvent.getMessage()
        );
    }
}
