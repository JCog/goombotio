package functions;

import net.dv8tion.jda.api.AccountType;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.exceptions.VerificationLevelException;
import util.Settings;

import javax.security.auth.login.LoginException;

import static java.lang.System.out;

public class DiscordBotController {
    private static DiscordBotController dbc = null;
    private JDA jda;

    public static DiscordBotController getInstance() {
        if (dbc == null) {
            dbc = new DiscordBotController();
        }
        return dbc;
    }

    public void init() {
        try {
            JDABuilder builder = new JDABuilder(AccountType.BOT);
            builder.setToken(Settings.getDiscordToken());
            jda = builder.build().awaitReady();
            out.println("Goombotio login to Discord successful.");
        }
        catch (LoginException | InterruptedException e) {
            out.println("Goombotio login to Discord unsuccessful:");
            e.printStackTrace();
        }
    }

    public void close() {
        jda.shutdown();
    }

    public void sendMessage(String channelName, String message) {
        TextChannel channel;
        try {
            channel = jda.getTextChannelsByName(channelName, true).get(0);
        }
        catch (IndexOutOfBoundsException e) {
            out.println(String.format("ERROR: discord channel \"#%s\" does not exist", channelName));
            return;
        }

        try {
            channel.sendMessage(message).queue();
        }
        catch (InsufficientPermissionException e) {
            out.println("ERROR: insufficient write privileges in this channel");
        }
        catch (VerificationLevelException e) {
            out.println("ERROR: verification failed");
        }
        catch (IllegalArgumentException e) {
            out.println("ERROR: provided text is null, empty, or longer than 2000 characters");
        }
        catch (UnsupportedOperationException e) {
            out.println("ERROR: this is a private channel and both the currently logged in account and the target user are bots");
        }
    }

    public void editMostRecentMessage(String channelName, String message) {
        TextChannel channel = jda.getTextChannelsByName(channelName, true).get(0);
        long messageId = channel.getLatestMessageIdLong();
        channel.editMessageById(messageId, message).queue();
    }

    public String getMostRecentMessageContents(String channelName) {
        TextChannel channel = jda.getTextChannelsByName(channelName, true).get(0);
        String messageId = channel.getLatestMessageId();
        return channel.retrieveMessageById(messageId).complete().getContentDisplay();
    }

    public boolean hasRecentMessageContents(String channelName) {
        TextChannel channel = jda.getTextChannelsByName(channelName, true).get(0);
        return channel.hasLatestMessage();
    }
}
