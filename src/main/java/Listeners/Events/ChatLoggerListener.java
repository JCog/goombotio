package Listeners.Events;

import Util.ChatLogger;
import com.gikk.twirk.events.TwirkListener;
import com.gikk.twirk.types.twitchMessage.TwitchMessage;
import com.gikk.twirk.types.users.TwitchUser;

public class ChatLoggerListener implements TwirkListener {
    private final ChatLogger chatLogger;
    
    public ChatLoggerListener(ChatLogger chatLogger) {
        this.chatLogger = chatLogger;
    }
    
    @Override
    public void onPrivMsg(TwitchUser sender, TwitchMessage message) {
        chatLogger.logMessage(sender, message);
    }
    
    
}
