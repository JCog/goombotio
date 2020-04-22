package Functions;

import APIs.SpeedrunApi;
import Listeners.Commands.*;
import Listeners.Events.*;
import Util.ChatLogger;
import Util.Database.GoombotioDb;
import Util.ReportBuilder;
import Util.StreamStatsInterface;
import Util.TwirkInterface;
import com.gikk.twirk.events.TwirkListener;
import com.github.twitch4j.TwitchClient;
import com.github.twitch4j.TwitchClientBuilder;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

import static java.lang.System.out;

public class MainBotController {
    private static final boolean VERBOSE_MODE = false;
    private static final int SOCIAL_INTERVAL_LENGTH = 20;
    
    private static MainBotController instance = null;
    
    private final TwirkInterface twirk;
    private final TwitchClient twitchClient;
    private final StreamInfo streamInfo;
    private final StatsTracker statsTracker;
    private final SocialScheduler socialScheduler;
    private final SubPointUpdater subPointUpdater;
    private final DiscordBotController dbc;
    private final ViewerQueueManager vqm;
    private final ChatLogger chatLogger;
    
    private MainBotController(String stream, String authToken, String discordToken, String channel, String nick, String oauth) {
        this.twirk = new TwirkInterface(channel, nick, oauth, VERBOSE_MODE);
        this.twitchClient = TwitchClientBuilder.builder().withEnableHelix(true).build();
        streamInfo = new StreamInfo(stream, twitchClient, authToken);
        statsTracker = new StatsTracker(twirk, twitchClient, streamInfo, stream, authToken, 60*1000);
        socialScheduler = new SocialScheduler(twirk, SOCIAL_INTERVAL_LENGTH, nick);
        subPointUpdater = new SubPointUpdater();
        vqm = new ViewerQueueManager(twirk);
        chatLogger = new ChatLogger();
        dbc = DiscordBotController.getInstance();
        dbc.init(discordToken);
    }
    
    public static MainBotController getInstance(String stream, String authToken, String discordToken, String channel, String nick, String oauth) {
        if (instance == null) {
            instance = new MainBotController(stream, authToken, discordToken, channel, nick, oauth);
        }
        return instance;
    }
    
    public void run() {
        streamInfo.startTracker();
        statsTracker.start();
        socialScheduler.start();
        subPointUpdater.start();
        addAllListeners();
        twirk.connect();
        checkSrcCert();
    
        out.println("Goombotio is ready.");
        
        String line;
        Scanner scanner = new Scanner(new InputStreamReader(System.in, StandardCharsets.UTF_8));
        while( !(line = scanner.nextLine()).matches(".quit") ) {
            twirk.channelMessage(line);
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
        chatLogger.close();
        twitchClient.close();
        twirk.close();
        dbc.close();
    }
    
    private void addAllListeners() {
        //setup
        SpeedySpinPredictionListener guessListener = new SpeedySpinPredictionListener();
        ViewerQueueJoinListener queueJoinListener = new ViewerQueueJoinListener(vqm);
        
        //connection handling
        addTwirkListener(getOnDisconnectListener(twirk));
        
        // Command Listeners
        addTwirkListener(new GenericCommandListener(twirk));
        addTwirkListener(new GoombotioCommandsListener(twirk));
        //addTwirkListener(new ModListener(twirk));
        addTwirkListener(guessListener);
        addTwirkListener(new SpeedySpinGameListener(twirk, guessListener));
        addTwirkListener(new SpeedySpinLeaderboardListener(twirk));
        addTwirkListener(queueJoinListener);
        addTwirkListener(new ViewerQueueManageListener(vqm, queueJoinListener));
        addTwirkListener(new WatchTimeListener(twirk));
        addTwirkListener(new WrListener(twirk, streamInfo));
    
        // General Listeners
        addTwirkListener(new ChatLoggerListener(chatLogger));
        addTwirkListener(new CloudListener(twirk));
        addTwirkListener(new EmoteListener());
        addTwirkListener(new PyramidListener(twirk));
        addTwirkListener(socialScheduler.getListener());
        addTwirkListener(new SubListener(twirk));
    }
    
    private void addTwirkListener(TwirkListener listener) {
        twirk.addIrcListener(listener);
    }
    
    private void removeTwirkListener(TwirkListener listener) {
        twirk.removeIrcListener(listener);
    }
    
    private static TwirkListener getOnDisconnectListener(final TwirkInterface twirk) {
        return new TwirkListener() {
            @Override
            public void onDisconnect() {
                do {
                    Calendar date = Calendar.getInstance();
                    int hour = date.get(Calendar.HOUR);
                    int minute = date.get(Calendar.MINUTE);
                    int second = date.get(Calendar.SECOND);
                    out.println(String.format("%02d:%02d:%02d - Trying to connect again in 10 seconds",
                            hour, minute, second));
                    try {
                        TimeUnit.SECONDS.sleep(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } while (!twirk.connect());
            }
        };
    }
    
    private void checkSrcCert() {
        if (!SpeedrunApi.certificateIsUpToDate()) {
            out.println("UPDATE THE SPEEDRUN.COM CERTIFICATE");
        }
    }
}
