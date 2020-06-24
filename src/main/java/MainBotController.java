import APIs.SpeedrunApi;
import Database.GoombotioDb;
import Functions.*;
import Listeners.Commands.*;
import Listeners.Commands.Preds.LeaderboardListener;
import Listeners.Commands.Preds.PredsGuessListener;
import Listeners.Commands.Preds.PredsManagerListener;
import Listeners.Events.*;
import Util.ChatLogger;
import Util.Settings;
import Util.TwirkInterface;
import Util.TwitchApi;
import com.gikk.twirk.events.TwirkListener;
import com.github.twitch4j.TwitchClient;
import com.github.twitch4j.TwitchClientBuilder;
import com.github.twitch4j.helix.domain.User;
import twitter4j.Twitter;
import twitter4j.TwitterFactory;
import twitter4j.conf.ConfigurationBuilder;

import java.util.Calendar;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;

import static java.lang.System.out;

public class MainBotController {
    private static final int SOCIAL_INTERVAL_LENGTH = 20;
    
    private static MainBotController instance = null;
    
    private final TwirkInterface twirk;
    private final TwitchClient twitchClient;
    private final TwitchApi twitchApi;
    private final SocialScheduler socialScheduler;
    private final StreamTracker streamTracker;
    //private final SubPointUpdater subPointUpdater;
    private final FollowLogger followLogger;
    private final DiscordBotController dbc;
    private final ViewerQueueManager vqm;
    private final ChatLogger chatLogger;
    private final Twitter twitter;
    
    private MainBotController() {
        chatLogger = new ChatLogger();
        twitter = getTwitterInstance();
        twitchClient = TwitchClientBuilder.builder()
                .withEnableHelix(true)
                .withClientId(Settings.getTwitchChannelClientId())
                .build();
        twitchApi = new TwitchApi(twitchClient);
        User botUser = twitchApi.getUserByUsername(Settings.getTwitchUsername());
        User streamerUser = twitchApi.getUserByUsername(Settings.getTwitchStream());
        twirk = new TwirkInterface(chatLogger, botUser);
        streamTracker = new StreamTracker(twirk, twitchApi);
        socialScheduler = new SocialScheduler(twirk, twitchApi, SOCIAL_INTERVAL_LENGTH);
        //subPointUpdater = new SubPointUpdater(twitchClient, twitchApi, botUser);
        followLogger = new FollowLogger(twitchApi, streamerUser.getId());
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
    
    public synchronized void run() {
        streamTracker.start();
        socialScheduler.start();
        //subPointUpdater.start();
        followLogger.start();
        addAllListeners();
        twirk.connect();
        checkSrcCert();
    
        out.println("Goombotio is ready.");
        
        //main loop
        try {
            new ConsoleCommandListener(twirk, dbc).run();
        }
        catch (NoSuchElementException nsee) {
            out.println("No console detected. Process must be killed manually");
            try {
                this.wait();
            }
            catch (InterruptedException ie) {
                ie.printStackTrace();
            }
        }
    }
    
    public void closeAll() {
        streamTracker.stop();
        socialScheduler.stop();
        //subPointUpdater.stop();
        followLogger.stop();
        GoombotioDb.getInstance().close();
        chatLogger.close();
        twitchClient.close();
        twirk.close();
        dbc.close();
    }
    
    private void addAllListeners() {
        //setup
        PredsGuessListener predsGuessListener = new PredsGuessListener();
        ViewerQueueJoinListener queueJoinListener = new ViewerQueueJoinListener(vqm);
        
        //connection handling
        addTwirkListener(getOnDisconnectListener(twirk));
        
        // Command Listeners
        addTwirkListener(new CommandManagerListener(twirk));
        addTwirkListener(new GenericCommandListener(twirk, twitchClient, twitchApi));
        //addTwirkListener(new ModListener(twirk));
        addTwirkListener(new LeaderboardListener(twirk, twitchApi));
        addTwirkListener(new QuoteListener(twirk));
        addTwirkListener(predsGuessListener);
        addTwirkListener(new PredsManagerListener(twirk, twitchApi, predsGuessListener));
        addTwirkListener(queueJoinListener);
        addTwirkListener(new ScheduledMessageManagerListener(twirk));
        addTwirkListener(new ViewerQueueManageListener(vqm, queueJoinListener));
        addTwirkListener(new WatchTimeListener(twirk));
        addTwirkListener(new WrListener(twirk, twitchApi));
    
        // General Listeners
        addTwirkListener(new ChatLoggerListener(chatLogger));
        addTwirkListener(new CloudListener(twirk));
        addTwirkListener(new EmoteListener());
        addTwirkListener(new LinkListener(twirk, twitchClient, twitter));
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
    
    private Twitter getTwitterInstance() {
        ConfigurationBuilder cb = new ConfigurationBuilder();
        cb.setDebugEnabled(true)
                .setOAuthConsumerKey(Settings.getTwitterConsumerKey())
                .setOAuthConsumerSecret(Settings.getTwitterConsumerSecret())
                .setOAuthAccessToken(Settings.getTwitterAccessToken())
                .setOAuthAccessTokenSecret(Settings.getTwitterAccessTokenSecret());
        TwitterFactory tf = new TwitterFactory(cb.build());
        return tf.getInstance();
    }
}
