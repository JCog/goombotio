import Database.GoombotioDb;
import Util.Settings;

import static java.lang.System.exit;
import static java.lang.System.out;

public class Main {
    public static void main(String[] args) {
        out.println("Starting...");
        Settings.init();
        GoombotioDb.getInstance().init(
                Settings.getDbHost(),
                Settings.getDbPort(),
                Settings.getDbUser(),
                Settings.getDbPassword()
        );
        MainBotController mainBotController = new MainBotController();
        
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            out.println("Stopping...");
            mainBotController.closeAll();
        }));
        
        //primary loop
        mainBotController.run();
        
        exit(0);
    }
}
