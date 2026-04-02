package dev.jcog.goombotio.listeners.events;

import com.github.twitch4j.chat.events.channel.ChannelMessageActionEvent;
import com.github.twitch4j.chat.events.channel.ChannelMessageEvent;
import com.github.twitch4j.chat.events.channel.ModAnnouncementEvent;
import dev.jcog.goombotio.listeners.TwitchEventListener;
import dev.jcog.goombotio.util.ChatLogger;

import java.util.List;

import static dev.jcog.goombotio.listeners.TwitchEventListener.EVENT_TYPE.*;

public class ChatLoggerListener implements TwitchEventListener {
    @Override
    public List<EVENT_TYPE> getEventTypes() {
        return List.of(CHANNEL_MESSAGE, ANNOUNCEMENT, CHANNEL_MESSAGE_ACTION);
    }

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
