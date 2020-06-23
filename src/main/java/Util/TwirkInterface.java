package Util;

import Listeners.Commands.CommandBase;
import Listeners.Commands.GenericCommandListener;
import com.gikk.twirk.Twirk;
import com.gikk.twirk.TwirkBuilder;
import com.gikk.twirk.events.TwirkListener;
import com.gikk.twirk.types.users.TwitchUser;
import com.github.twitch4j.helix.domain.User;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class TwirkInterface {
    private final String channel = Settings.getTwitchChannel();
    private final String nick = Settings.getTwitchUsername();
    private final String oauth = Settings.getTwitchBotOauth();
    private final Boolean silent = Settings.isSilentMode();
    private final Boolean verbose = Settings.isVerboseLogging();
    private final HashSet<TwirkListener> twirkListeners = new HashSet<>();
    
    private final String botDisplayName;
    private final long botId;
    private final ChatLogger chatLogger;
    
    private Twirk twirk;
    
    public TwirkInterface (ChatLogger chatLogger, User botUser) {
        this.chatLogger = chatLogger;
        botDisplayName = botUser.getDisplayName();
        botId = Long.parseLong(botUser.getId());
    }
    
    /**
     * Sends a message to twitch chat if the first non-whitespace character is not '/' to prevent commands
     * @param line
     */
    public void channelMessage(String line) {
        String output = line.trim();
        if (output.isEmpty()) {
            return;
        }
        String firstWord = output.split("\\s", 2)[0];
        if (firstWord.charAt(0) == '/') {
            System.out.println(String.format("Illegal command usage \"%s\"", firstWord));
        }
        else {
            sendMessage(output);
        }
    }
    
    /**
     * Sends a message to twitch chat with no restrictions on commands
     * @param line message to send
     */
    public void channelCommmand(String line) {
        String output = line.trim();
        if (output.isEmpty()) {
            return;
        }
        String firstWord = output.split("\\s", 2)[0];
        if (firstWord.charAt(0) == '/') {
            System.out.println(String.format("command message sent \"%s\"", output));
        }
        sendMessage(output);
    }
    
    public Set<String> getCommandPatterns() {
        HashSet<String> commands = new HashSet<>();
        for (TwirkListener listener : twirkListeners) {
            if(CommandBase.class.isAssignableFrom(listener.getClass()) && listener.getClass() != GenericCommandListener.class) {
                String[] commandWords = ((CommandBase) listener).getCommandWords().split("\\|");
                commands.addAll(Arrays.asList(commandWords));
            }
        }
        return commands;
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
    
    private void sendMessage(String message) {
        if (silent) {
            System.out.println("SILENT_CHAT: " + message);
        }
        else {
            twirk.channelMessage(message);
            chatLogger.logMessage(botId, botDisplayName, message);
        }
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
