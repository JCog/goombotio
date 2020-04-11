package Functions;

import Listeners.Commands.*;
import Listeners.Events.SubListener;
import Util.Database.GoombotioDb;
import Util.Database.SpeedySpinLeaderboard;
import Util.ReportBuilder;
import Util.StreamStatsInterface;
import com.gikk.twirk.Twirk;
import com.gikk.twirk.TwirkBuilder;
import com.gikk.twirk.events.TwirkListener;
import com.github.twitch4j.TwitchClient;
import com.github.twitch4j.TwitchClientBuilder;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class MainBotController {
    private static MainBotController instance = null;
    private Twirk twirk;
    private TwitchClient twitchClient;
    private StreamInfo streamInfo;
    private StatsTracker statsTracker;
    private SocialScheduler socialScheduler;
    private SubPointUpdater subPointUpdater;
    private DiscordBotController dbc;
    private ViewerQueueManager vqm;
    
    private static final boolean VERBOSE_MODE = false;
    private static final int SOCIAL_INTERVAL_LENGTH = 20;
    
    private MainBotController(String stream, String authToken, String discordToken, String channel, String nick, String oauth) throws IOException {
        this.twirk = new TwirkBuilder(channel, nick, oauth)
                .setVerboseMode(VERBOSE_MODE)
                .build();
        this.twitchClient = TwitchClientBuilder.builder().withEnableHelix(true).build();
        streamInfo = new StreamInfo(stream, twitchClient, authToken);
        statsTracker = new StatsTracker(twirk, twitchClient, streamInfo, stream, authToken, 60*1000);
        socialScheduler = new SocialScheduler(twirk, SOCIAL_INTERVAL_LENGTH, nick);
        subPointUpdater = new SubPointUpdater();
        vqm = new ViewerQueueManager(twirk);
        dbc = DiscordBotController.getInstance();
        dbc.init(discordToken);
    }
    
    public static MainBotController getInstance(String stream, String authToken, String discordToken, String channel, String nick, String oauth) throws IOException {
        if (instance == null) {
            instance = new MainBotController(stream, authToken, discordToken, channel, nick, oauth);
        }
        return instance;
    }
    
    public void run() throws IOException, InterruptedException {
        streamInfo.startTracker();
        statsTracker.start();
        socialScheduler.start();
        subPointUpdater.start();
        addAllListeners();
        twirk.connect();
    
        String line;
        Scanner scanner = new Scanner(new InputStreamReader(System.in, StandardCharsets.UTF_8));
        TwirkListener viewerQueueListener = new ViewerQueueListener(vqm);
        while( !(line = scanner.nextLine()).matches(".quit") ) {
            if(line.equals(".lb")) { //TODO: this is pretty hacky, should improve
                SpeedySpinLeaderboard lb = SpeedySpinLeaderboard.getInstance();
                lb.logPreviousTopMonthlyScorers();
            }
            else if (line.startsWith(".startqueue")) {
                String[] lineSplit = line.split(" ");
                int requestedCount = Integer.parseInt(lineSplit[1]);
                String message = line.substring(lineSplit[0].length() + lineSplit[1].length() + 2);
                vqm.startNewSession(requestedCount, message);
                addTwirkListener(viewerQueueListener);
            }
            else if (line.equals(".endqueue")) {
                removeTwirkListener(viewerQueueListener);
                vqm.closeCurrentSession();
            }
            else if (line.equals(".next")) {
                vqm.getNext();
            }
            else {
                twirk.channelMessage(line);
            }
        }
        scanner.close();
    }
    
    public void closeAll() {
        streamInfo.stopTracker();
        statsTracker.stop();
        socialScheduler.stop();
        subPointUpdater.stop();
        StreamStatsInterface.saveStreamStats(streamInfo, statsTracker); //run before storing minutes for accurate new viewers
        statsTracker.storeAllMinutes();
        ReportBuilder.generateReport(streamInfo, statsTracker);
        GoombotioDb.getInstance().close();
        twirk.close();
    }
    
    private void addAllListeners() {
        addTwirkListener(getOnDisconnectListener(twirk));
        addTwirkListener(new SpeedySpinGameListener(twirk));
        addTwirkListener(new SpeedySpinLeaderboardListener(twirk));
        //addTwirkListener(new ModListener(twirk));
        addTwirkListener(new WrListener(twirk, streamInfo));
        addTwirkListener(new SubListener(twirk));
        addTwirkListener(new GoombotioCommandsListener(twirk));
        addTwirkListener(new GenericCommandListener(twirk));
        addTwirkListener(new WatchTimeListener(twirk));
    }
    
    private void addTwirkListener(TwirkListener listener) {
        twirk.addIrcListener(listener);
    }
    
    private void removeTwirkListener(TwirkListener listener) {
        twirk.removeIrcListener(listener);
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
