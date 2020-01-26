import Functions.DiscordBotController;
import Functions.MainBotController;

import javax.security.auth.login.LoginException;
import java.io.IOException;

import static java.lang.System.exit;
import static java.lang.System.out;

public class Main {
    /**
     * @param args 0 - stream
     *             1 - bot username
     *             2 - bot authToken
     *             3 - discord token
     * @throws IOException
     * @throws InterruptedException
     */
    public static void main(String[] args) throws IOException, InterruptedException {
        final String STREAM = args[0];
        final String AUTH_TOKEN = args[2];
        final String DISCORD_TOKEN = args[3];
        final String CHANNEL = '#' + STREAM;
        final String NICK = args[1];
        final String OAUTH = "oauth:" + AUTH_TOKEN;
        
        MainBotController mainBotController = MainBotController.getInstance(STREAM, AUTH_TOKEN, CHANNEL, NICK, OAUTH);
        DiscordBotController discordBotController = new DiscordBotController(DISCORD_TOKEN);
        
        out.println("Goombotio is ready.");
        
        //primary loop
        mainBotController.run();
        
        out.println("Stopping...");
        mainBotController.closeAll();
        exit(0);
    }
}
