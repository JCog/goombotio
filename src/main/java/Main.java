import Util.Settings;

import static java.lang.System.exit;
import static java.lang.System.out;

public class Main {
    public static void main(String[] args) {
        out.println("Starting...");
        Settings.init();
        MainBotController mainBotController = MainBotController.getInstance();
        
        //primary loop
        mainBotController.run();
        
        out.println("Stopping...");
        mainBotController.closeAll();
        exit(0);
    }
}
