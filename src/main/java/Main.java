import Util.Settings;

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
        
        Settings.init();
        
        MainBotController mainBotController = MainBotController.getInstance();
        
        //primary loop
        mainBotController.run();
        
        out.println("Stopping...");
        mainBotController.closeAll();
        exit(0);
    }
}
