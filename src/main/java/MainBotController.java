import APIs.SpeedrunApi;
import Functions.*;
import Listeners.Commands.*;
import Listeners.Events.*;
import Util.*;
import Util.Database.GoombotioDb;
import com.gikk.twirk.events.TwirkListener;
import com.github.twitch4j.TwitchClient;
import com.github.twitch4j.TwitchClientBuilder;
import com.github.twitch4j.helix.domain.User;
import com.github.twitch4j.helix.domain.UserList;

import java.util.Calendar;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import static java.lang.System.out;

public class MainBotController {
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
    
    private MainBotController() {
        chatLogger = new ChatLogger();
        this.twitchClient = TwitchClientBuilder.builder().withEnableHelix(true).build();
        this.twirk = new TwirkInterface(chatLogger, getBotUser(Settings.getTwitchUsername()));
        streamInfo = new StreamInfo(twitchClient);
        statsTracker = new StatsTracker(twirk, twitchClient, streamInfo, 60*1000);
        socialScheduler = new SocialScheduler(twirk, SOCIAL_INTERVAL_LENGTH);
        subPointUpdater = new SubPointUpdater();
        vqm = new ViewerQueueManager(twirk);
        dbc = DiscordBotController.getInstance();
        dbc.init();
    }
    
    public static MainBotController getInstance() {
        if (instance == null) {
            instance = new MainBotController();
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
        
        //main loop
        new ConsoleCommandListener(twirk, dbc).run();
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
        addTwirkListener(new GenericCommandListener(twirk, twitchClient, streamInfo));
        addTwirkListener(new GoombotioCommandsListener(twirk));
        //addTwirkListener(new ModListener(twirk));
        addTwirkListener(guessListener);
        addTwirkListener(new QuoteListener(twirk));
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
        addTwirkListener(new LinkListener(twirk, twitchClient));
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
    
    private User getBotUser(String nick) {
        UserList result = twitchClient.getHelix().getUsers(Settings.getTwitchAuthToken(), null, Collections.singletonList(nick)).execute();
        return result.getUsers().get(0);
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
