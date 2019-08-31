import Functions.StreamInfo;
import Functions.ViewerTracker;
import Listeners.ModListener;
import Listeners.SpeedySpinGameListener;
import Listeners.SpeedySpinLeaderboardListener;
import Util.Database.SpeedySpinLeaderboard;
import com.gikk.twirk.Twirk;
import com.gikk.twirk.TwirkBuilder;
import com.gikk.twirk.events.TwirkListener;
import com.github.twitch4j.TwitchClient;
import com.github.twitch4j.TwitchClientBuilder;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

import static java.lang.System.exit;
import static java.lang.System.out;

public class Main {
    public static void main(String[] args) throws IOException, InterruptedException{
        final String STREAM = args[0];
        final String CHANNEL = '#' + args[0];
        final String NICK = args[1];
        final String OAUTH = args[2];
        final boolean VERBOSE_MODE = false;
        final Twirk twirk;
        final TwitchClient twitchClient;

        twitchClient = TwitchClientBuilder.builder().withEnableHelix(true).build();

        Scanner scanner = new Scanner(new InputStreamReader(System.in, StandardCharsets.UTF_8));

        twirk = new TwirkBuilder(CHANNEL,NICK, OAUTH)
                .setVerboseMode(VERBOSE_MODE)
                .build();
        twirk.addIrcListener(getOnDisconnectListener(twirk));
        twirk.addIrcListener(new SpeedySpinGameListener(twirk));
        twirk.addIrcListener(new SpeedySpinLeaderboardListener(twirk));
        twirk.addIrcListener(new ModListener(twirk));
        twirk.connect();

        StreamInfo streamInfo = new StreamInfo(STREAM, twitchClient);
        streamInfo.startTracker();

        ViewerTracker viewerTracker = new ViewerTracker(twirk, twitchClient, streamInfo, 60*1000);
        viewerTracker.start();

        //SocialScheduler socialScheduler = new SocialScheduler(twirk);
        //socialScheduler.start();

        String line;

        out.println("Goombotio is ready.");
        //primary loop
        while( !(line = scanner.nextLine()).matches(".quit") ) {
            if(line.equals(".lb")) { //TODO: this is pretty hacky, should improve
                SpeedySpinLeaderboard lb = new SpeedySpinLeaderboard();
                lb.logPreviousTopMonthlyScorers();
            }
            else {
                twirk.channelMessage(line);
            }
        }

        streamInfo.stopTracker();
        viewerTracker.stop();
        viewerTracker.printViewersByViewTime();
        viewerTracker.storeAllMinutes();
        //socialScheduler.stop();
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
