package Functions;

import net.dv8tion.jda.api.AccountType;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;

import static java.lang.System.out;

import javax.security.auth.login.LoginException;

public class DiscordBotController {
    private JDA jda;
    
    public DiscordBotController(String token) throws LoginException, InterruptedException {
        JDABuilder builder = new JDABuilder(AccountType.BOT);
        builder.setToken(token);
        jda = builder.build().awaitReady();
        out.println("Goombotio login to Discord successful.");
    }
}
