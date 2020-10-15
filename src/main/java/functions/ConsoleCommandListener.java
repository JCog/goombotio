package functions;

import util.TwirkInterface;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.NoSuchElementException;
import java.util.Scanner;

import static java.lang.System.out;

public class ConsoleCommandListener {
    private static final String DISCORD_COMMAND = ".discord";
    private static final String TWITCH_COMMAND = ".twitch";
    private static final String QUIT_COMMAND = ".quit";

    private final TwirkInterface twirk;
    private final DiscordBotController dbc;

    public ConsoleCommandListener(TwirkInterface twirk, DiscordBotController dbc) {
        this.twirk = twirk;
        this.dbc = dbc;
    }

    public void run() throws NoSuchElementException {
        //this should probably be made cleaner if I ever need more console commands, but honestly it's fine for now
        String line;
        Scanner scanner = new Scanner(new InputStreamReader(System.in, StandardCharsets.UTF_8));
        while (!(line = scanner.nextLine().trim()).equals(QUIT_COMMAND)) {
            String[] lineSplit = line.split(" ");
            String command = lineSplit[0];
            if (lineSplit.length == 2 && command.equals(DISCORD_COMMAND)) {
                String channel = lineSplit[1];
                out.print(String.format("Enter message for #%s: ", channel));
                dbc.sendMessage(channel, scanner.nextLine());
            }
            else if (command.equals(TWITCH_COMMAND)) {
                int start = line.indexOf(' ') + 1;
                if (line.length() > start) {
                    twirk.channelMessage(line.substring(start));
                    out.println("Message sent to twitch chat");
                }
                else {
                    out.println("ERROR: No message");
                }
            }
        }
        scanner.close();
    }
}
