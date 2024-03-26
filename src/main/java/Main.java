import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import static java.lang.System.exit;
import static java.lang.System.out;

public class Main {
    public static void main(String[] args) {
        long startTime = System.currentTimeMillis();
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z").withZone(ZoneId.systemDefault());
        out.println(dtf.format(Instant.now()));
        out.println("Starting...");
        MainBotController mainBotController = new MainBotController();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            out.print("Stopping... ");
            mainBotController.closeAll();
            out.println("done.\n");
        }));

        //primary loop
        mainBotController.run(startTime);

        exit(0);
    }
}
