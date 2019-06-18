import Functions.ViewerTracker;
import Listeners.ModListener;
import Listeners.SpeedySpinGameListener;
import Listeners.SpeedySpinLeaderboardListener;
import com.gikk.twirk.Twirk;
import com.gikk.twirk.TwirkBuilder;
import com.gikk.twirk.events.TwirkListener;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

import static java.lang.System.exit;
import static java.lang.System.out;

public class Main {
    public static void main(String[] args) throws IOException, InterruptedException{
        final String CHANNEL = '#' + args[0];
        final String NICK = args[1];
        final String OAUTH = args[2];
        final boolean VERBOSE_MODE = false;
        final Twirk twirk;

        Scanner scanner = new Scanner(new InputStreamReader(System.in, StandardCharsets.UTF_8));

        twirk = new TwirkBuilder(CHANNEL,NICK, OAUTH)
                .setVerboseMode(VERBOSE_MODE)
                .build();
        twirk.addIrcListener(getOnDisconnectListener(twirk));
        twirk.addIrcListener(new SpeedySpinGameListener(twirk));
        twirk.addIrcListener(new SpeedySpinLeaderboardListener(twirk));
        twirk.addIrcListener(new ModListener(twirk));
        twirk.connect();


        ViewerTracker viewerTracker = new ViewerTracker(twirk, 60*1000);
        viewerTracker.start();

        String line;

        out.println("Goombotio is ready.");
        //primary loop
        while( !(line = scanner.nextLine()).matches(".quit") ) {
            twirk.channelMessage(line);
        }

        viewerTracker.stop();
        viewerTracker.printViewersByViewTime();
        scanner.close();
        twirk.close();
        exit(0);
    }

    private static TwirkListener getOnDisconnectListener(final Twirk twirk) {
        return new TwirkListener() {
            @Override
            public void onDisconnect() {
                try {
                    if(!twirk.connect()) {
                        twirk.close();
                    }
                }
                catch (IOException e) {
                    twirk.close();
                }
                catch (InterruptedException e) {
                    //continue
                }
            }
        };
    }
}
