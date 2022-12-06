package util;

import com.gikk.twirk.Twirk;
import com.gikk.twirk.TwirkBuilder;
import com.gikk.twirk.events.TwirkListener;
import com.github.philippheuer.credentialmanager.domain.OAuth2Credential;
import com.github.twitch4j.TwitchClient;
import com.github.twitch4j.TwitchClientBuilder;
import com.github.twitch4j.helix.domain.User;

import java.io.IOException;
import java.util.HashSet;

public class TwirkInterface {
    private final HashSet<TwirkListener> twirkListeners = new HashSet<>();
    private final String channel;
    private final String nick;
    private final String oauth;
    private final Boolean silent;
    private final Boolean verbose;

    private final String botDisplayName;
    private final long botId;
    private final ChatLogger chatLogger;

    private Twirk twirk;
    private final TwitchClient twitchClient;

    public TwirkInterface(
            ChatLogger chatLogger,
            User botUser,
            String channel,
            String nick,
            String oauth,
            Boolean silent,
            Boolean verbose
    ) {
        this.chatLogger = chatLogger;
        this.channel = channel;
        this.nick = nick;
        this.oauth = oauth;
        this.silent = silent;
        this.verbose = verbose;
        botDisplayName = botUser.getDisplayName();
        botId = Long.parseLong(botUser.getId());
        twitchClient = TwitchClientBuilder.builder()
                .withEnableChat(true)
                .withChatAccount(new OAuth2Credential("twitch", oauth))
                .build();
    }

    /**
     * Sends a message to twitch chat if the first non-whitespace character is not '/' or '.' to prevent commands
     *
     * @param line message to send
     */
    public void channelMessage(String line) {
        String output = line.trim();
        if (output.isEmpty()) {
            return;
        }
        String firstWord = output.split("\\s", 2)[0];
        if (firstWord.charAt(0) == '/' || firstWord.charAt(0) == '.') {
            System.out.printf("Illegal command usage \"%s\"%n", firstWord);
        }
        else {
            sendMessage(output);
        }
    }

    /**
     * Sends a message to twitch chat with no restrictions on commands
     *
     * @param line message to send
     */
    public void channelCommand(String line) {
        String output = line.trim();
        if (output.isEmpty()) {
            return;
        }
        String firstWord = output.split("\\s", 2)[0];
        if (firstWord.charAt(0) == '/') {
            System.out.printf("command message sent \"%s\"%n", output);
        }
        sendMessage(output);
    }

    public void whisper(String username, String message) {
        twitchClient.getChat().sendPrivateMessage(username, message);
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

    public boolean connect() {
        try {
            getNewTwirk();
            twirk.connect();
        }
        catch (IOException | InterruptedException e) {
            System.out.println("Twirk failed to reconnect");
            return false;
        }
        twitchClient.getChat().joinChannel(channel);
        System.out.printf("Twirk connected to %s successfully%n", channel);
        return true;
    }

    public void close() {
        twirk.close();
    }

    private void sendMessage(String message) {
        if (silent) {
            System.out.println("SILENT_CHAT: " + message);
        }
        else {
            twitchClient.getChat().sendMessage(channel, message);
            chatLogger.logMessage(botId, botDisplayName, message);
        }
    }

    private void getNewTwirk() {
        twirk = new TwirkBuilder(channel, nick, oauth)
                .setVerboseMode(verbose)
                .build();
        for (TwirkListener listener : twirkListeners) {
            twirk.addIrcListener(listener);
        }
    }
}
