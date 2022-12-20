import static java.lang.System.exit;
import static java.lang.System.out;

public class Main {
    public static void main(String[] args) {
        long startTime = System.currentTimeMillis();
        out.println("Starting...");
        MainBotController mainBotController = new MainBotController();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            out.println("Stopping...");
            mainBotController.closeAll();
        }));

        //primary loop
        mainBotController.run(startTime);

        exit(0);
    }
}
