import Functions.StreamInfo;
import Functions.StatsTracker;
import Listeners.ModListener;
import Listeners.SpeedySpinGameListener;
import Listeners.SpeedySpinLeaderboardListener;
import Listeners.WrListener;
import Util.Database.SpeedySpinLeaderboard;
import Util.ReportBuilder;
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
    /**
     * @param args 0 - stream
     *             1 - bot username
     *             2 - bot authToken
     * @throws IOException
     * @throws InterruptedException
     */
    public static void main(String[] args) throws IOException, InterruptedException{
        final String STREAM = args[0];
        final String AUTH_TOKEN = args[2];
        final String CHANNEL = '#' + STREAM;
        final String NICK = args[1];
        final String OAUTH = "oauth:" + AUTH_TOKEN;
        final boolean VERBOSE_MODE = false;
        final Twirk twirk;
        final TwitchClient twitchClient;

        twitchClient = TwitchClientBuilder.builder().withEnableHelix(true).build();

        StreamInfo streamInfo = new StreamInfo(STREAM, twitchClient, AUTH_TOKEN);
        streamInfo.startTracker();

        Scanner scanner = new Scanner(new InputStreamReader(System.in, StandardCharsets.UTF_8));

        twirk = new TwirkBuilder(CHANNEL,NICK, OAUTH)
                .setVerboseMode(VERBOSE_MODE)
                .build();
        twirk.addIrcListener(getOnDisconnectListener(twirk));
        twirk.addIrcListener(new SpeedySpinGameListener(twirk));
        twirk.addIrcListener(new SpeedySpinLeaderboardListener(twirk));
        twirk.addIrcListener(new ModListener(twirk));
        twirk.addIrcListener(new WrListener(twirk, streamInfo));
        twirk.connect();

        StatsTracker statsTracker = new StatsTracker(twirk, twitchClient, streamInfo, STREAM, AUTH_TOKEN, 60*1000);
        statsTracker.start();

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

        out.println("Stopping...");
        streamInfo.stopTracker();
        statsTracker.stop();
        statsTracker.storeAllMinutes();
        //socialScheduler.stop();
        ReportBuilder.generateReport(streamInfo, statsTracker);
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
