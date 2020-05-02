import static java.lang.System.exit;
import static java.lang.System.out;

public class Main {
    /**
     * @param args 0 - stream
     *             1 - bot username
     *             2 - bot authToken
     *             3 - discord token
     *             4 - youtube api key
     */
    public static void main(String[] args) {
        final String STREAM = args[0];
        final String AUTH_TOKEN = args[2];
        final String DISCORD_TOKEN = args[3];
        final String YOUTUBE_API_KEY = args[4];
        final String CHANNEL = '#' + STREAM;
        final String NICK = args[1];
        final String OAUTH = "oauth:" + AUTH_TOKEN;
        
        MainBotController mainBotController = MainBotController.getInstance(STREAM, AUTH_TOKEN, DISCORD_TOKEN, CHANNEL, NICK, OAUTH, YOUTUBE_API_KEY);
        
        //primary loop
        mainBotController.run();
        
        out.println("Stopping...");
        mainBotController.closeAll();
        exit(0);
    }
}
