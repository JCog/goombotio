package Util;

import com.gikk.twirk.Twirk;
import com.gikk.twirk.TwirkBuilder;
import com.gikk.twirk.events.TwirkListener;
import com.gikk.twirk.types.users.TwitchUser;
import com.github.twitch4j.helix.domain.User;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class TwirkInterface {
    private final String channel;
    private final String nick;
    private final String oauth;
    private final ChatLogger chatLogger;
    private final Boolean verbose;
    private final HashSet<TwirkListener> twirkListeners;
    private final String botDisplayName;
    private final long botId;
    
    private Twirk twirk;
    
    public TwirkInterface (ChatLogger chatLogger, User botUser) {
        this.channel = Settings.getTwitchChannel();
        this.nick = Settings.getTwitchUsername();
        this.oauth = Settings.getTwitchOauth();
        this.verbose = Settings.isVerbose();
        this.chatLogger = chatLogger;
        botDisplayName = botUser.getDisplayName();
        botId = Long.parseLong(botUser.getId());
        twirkListeners = new HashSet<>();
    }
    
    public void channelMessage(String line) {
        twirk.channelMessage(line);
        chatLogger.logMessage(botId, botDisplayName, line);
    }
    
    public void priorityMessage(String message) {
        twirk.priorityChannelMessage(message);
    }
    
    public void whisper(String username, String message) {
        twirk.whisper(username, message);
    }
    
    public void whisper(TwitchUser receiver, String message) {
        twirk.whisper(receiver, message);
    }
    
    public void addIrcListener(TwirkListener listener) {
        twirkListeners.add(listener);
        if (twirk != null && twirk.isConnected()) {
            twirk.addIrcListener(listener);
        }
    }
    
    public void removeIrcListener(TwirkListener listener) {
        twirk.removeIrcListener(listener);
        twirkListeners.remove(listener);
    }
    
    public Set<String> getUsersOnline() {
        return twirk.getUsersOnline();
    }
    
    public Set<String> getModsOnline() {
        return twirk.getModsOnline();
    }
    
    public boolean connect() {
        try {
            getNewTwirk();
            twirk.connect();
        }
        catch (IOException|InterruptedException e) {
            System.out.println("Twirk failed to reconnect");
            return false;
        }
        System.out.println(String.format("Twirk connected to %s successfully", channel));
        return true;
    }
    
    public void serverMessage(String message) {
        twirk.serverMessage(message);
    }
    
    public boolean isConnected() {
        return twirk.isConnected();
    }
    
    public boolean isDisposed() {
        return twirk.isDisposed();
    }
    
    public String getNick() {
        return twirk.getNick();
    }
    
    public synchronized void disconnect() {
        twirk.disconnect();
    }
    
    public void close() {
        twirk.close();
    }
    
    private void getNewTwirk() throws IOException {
        twirk = new TwirkBuilder(channel, nick, oauth)
                .setVerboseMode(verbose)
                .build();
        for (TwirkListener listener : twirkListeners) {
            twirk.addIrcListener(listener);
        }
    }
}
