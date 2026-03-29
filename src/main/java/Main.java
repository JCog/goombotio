import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.StartupException;

import static java.lang.System.exit;

public class Main {
    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        long startTime = System.currentTimeMillis();
        log.info("Beginning initial startup");
        MainBotController mainBotController;
        try {
            mainBotController = new MainBotController();
        } catch (StartupException e) {
            log.error(e.getMessage());
            log.info("Stopping...");
            exit(1);
            return;
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Shutdown hook detected. Shutting down...");
            mainBotController.closeAll();
            log.info("Shutdown complete.");
        }));

        //primary loop
        mainBotController.run(startTime);

        exit(0);
    }
}
