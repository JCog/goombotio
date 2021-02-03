package functions;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;

import javax.security.auth.login.LoginException;

import static java.lang.System.out;

public class DiscordBotController {
    private static final String INSUFFICIENT_PERMISSION_ERROR = "ERROR: insufficient write privileges in this channel";
    private static final String ILLEGAL_ARGUMENT_ERROR = "ERROR: provided text is null, empty, or longer than 2000 characters";
    private static final String UNSUPPORTED_OPERATION_ERROR = "ERROR: this is a private channel and both the currently logged in account and the target user are bots";

    private static DiscordBotController dbc = null;
    private JDA jda;

    public static DiscordBotController getInstance() {
        if (dbc == null) {
            dbc = new DiscordBotController();
        }
        return dbc;
    }

    public void init(String token) {
        try {
            jda = JDABuilder.createDefault(token).build().awaitReady();
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
            out.printf("ERROR: discord channel \"#%s\" does not exist%n", channelName);
            return;
        }

        try {
            channel.sendMessage(message).queue();
        }
        catch (InsufficientPermissionException e) {
            out.println(INSUFFICIENT_PERMISSION_ERROR);
        }
        catch (IllegalArgumentException e) {
            out.println(ILLEGAL_ARGUMENT_ERROR);
        }
        catch (UnsupportedOperationException e) {
            out.println(UNSUPPORTED_OPERATION_ERROR);
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
