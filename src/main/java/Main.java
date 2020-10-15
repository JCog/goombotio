import util.Settings;

import static java.lang.System.exit;
import static java.lang.System.out;

public class Main {
    public static void main(String[] args) {
        out.println("Starting...");
        Settings.init();
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
