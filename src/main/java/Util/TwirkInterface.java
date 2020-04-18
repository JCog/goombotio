package Util;

import com.gikk.twirk.Twirk;
import com.gikk.twirk.TwirkBuilder;
import com.gikk.twirk.events.TwirkListener;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class TwirkInterface {
    private final String channel;
    private final String nick;
    private final String oauth;
    private final Boolean verbose;
    private final HashSet<TwirkListener> twirkListeners;
    
    private Twirk twirk;
    
    public TwirkInterface (String channel, String nick, String oauth, boolean verbose) {
        this.channel = channel;
        this.nick = nick;
        this.oauth = oauth;
        this.verbose = verbose;
        twirkListeners = new HashSet<>();
    }
    
    public void channelMessage(String line) {
        twirk.channelMessage(line);
    }
    
    public void whisper(String username, String message) {
        twirk.whisper(username, message);
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
