package functions;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.exceptions.InvalidTokenException;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.StartupException;

public class DiscordBotController {
    private static final Logger log = LoggerFactory.getLogger(DiscordBotController.class);

    private final JDA jda;

    public DiscordBotController(String token, ListenerAdapter listenerAdapter) {
        try {
            log.info("Establishing Discord connection...");
            jda = JDABuilder.createDefault(token)
                    .addEventListeners(listenerAdapter)
                    .build()
                    .awaitReady();
        } catch (InvalidTokenException | InterruptedException e) {
            throw new StartupException("unable to authenticate with discord");
        }
        log.info("Discord connection established.");
    }

    public void close() {
        jda.shutdown();
    }

    public void sendMessage(String channelName, String message) {
        TextChannel channel;
        try {
            channel = jda.getTextChannelsByName(channelName, true).get(0);
        } catch (IndexOutOfBoundsException e) {
            log.error("Discord channel \"#{}\" does not exist", channelName);
            return;
        }

        try {
            channel.sendMessage(message).queue();
        } catch (InsufficientPermissionException e) {
            log.error("insufficient write privileges to write in #{}", channelName);
        } catch (IllegalArgumentException e) {
            log.error("provided text is null, empty, or longer than 2000 characters");
        } catch (UnsupportedOperationException e) {
            log.error(
                    "#{} is a private channel and both the currently logged in account and the target user are bots",
                    channelName
            );
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
        return !channel.getLatestMessageId().equals("0");
    }
}
