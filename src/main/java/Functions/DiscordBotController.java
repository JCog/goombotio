package Functions;

import net.dv8tion.jda.api.AccountType;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.TextChannel;

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
    
    public void init(String token) {
        try {
            JDABuilder builder = new JDABuilder(AccountType.BOT);
            builder.setToken(token);
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
        TextChannel channel =  jda.getTextChannelsByName(channelName, true).get(0);
        channel.sendMessage(message).queue();
    }
    
    public void editMostRecentMessage(String channelName, String message) {
        TextChannel channel =  jda.getTextChannelsByName(channelName, true).get(0);
        long messageId = channel.getLatestMessageIdLong();
        channel.editMessageById(messageId, message).queue();
    }
    
    public String getMostRecentMessageContents(String channelName) {
        TextChannel channel =  jda.getTextChannelsByName(channelName, true).get(0);
        String messageId = channel.getLatestMessageId();
        return channel.retrieveMessageById(messageId).complete().getContentDisplay();
    }
    
    public boolean hasRecentMessageContents(String channelName) {
        TextChannel channel =  jda.getTextChannelsByName(channelName, true).get(0);
        return channel.hasLatestMessage();
    }
}
