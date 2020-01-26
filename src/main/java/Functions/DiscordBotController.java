package Functions;

import net.dv8tion.jda.api.AccountType;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.TextChannel;

import static java.lang.System.out;

import javax.security.auth.login.LoginException;

public class DiscordBotController {
    private JDA jda;
    
    public DiscordBotController(String token) {
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
    
    public void sendMessage(String channelName, String message) throws InterruptedException {
        TextChannel channel =  jda.getTextChannelsByName(channelName, true).get(0);
        channel.sendMessage(message).queue();
    }
}
