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

    private final Twitter twitter = getTwitterInstance();
    private final DiscordBotController discordBotController = DiscordBotController.getInstance();
    private final ChatLogger chatLogger = new ChatLogger();
    private final TwitchClient
            twitchClient = TwitchClientBuilder.builder()
            .withEnableHelix(true)
            .withClientId(Settings.getTwitchChannelClientId())
            .build();
    private final TwitchApi twitchApi = new TwitchApi(twitchClient);
    private final User botUser = twitchApi.getUserByUsername(Settings.getTwitchUsername());
    private final User streamerUser = twitchApi.getUserByUsername(Settings.getTwitchStream());
    private final TwirkInterface twirk = new TwirkInterface(chatLogger, botUser);
    private final CloudListener cloudListener = new CloudListener(twirk);
    private final StreamTracker streamTracker = new StreamTracker(twirk, twitchApi, streamerUser, cloudListener);
    private final SocialScheduler socialScheduler = new SocialScheduler(twirk, twitchApi, botUser, SOCIAL_INTERVAL_LENGTH);
    private final FollowLogger followLogger = new FollowLogger(twitchApi, streamTracker, streamerUser);
    private final ViewerQueueManager viewerQueueManager = new ViewerQueueManager(twirk);
    
    public synchronized void run() {
        discordBotController.init();
        socialScheduler.start();
        followLogger.start();
        addAllListeners();
        twirk.connect();
        streamTracker.start();
        checkSrcCert();
    
        out.println("Goombotio is ready.");
        
        //main loop
        try {
            new ConsoleCommandListener(twirk, discordBotController).run();
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
        followLogger.stop();
        GoombotioDb.getInstance().close();
        chatLogger.close();
        twitchClient.close();
        twirk.close();
        discordBotController.close();
    }
    
    private void addAllListeners() {
        //setup
        PredsGuessListener predsGuessListener = new PredsGuessListener();
        ViewerQueueJoinListener queueJoinListener = new ViewerQueueJoinListener(viewerQueueManager);
        
        //connection handling
        twirk.addIrcListener(getOnDisconnectListener(twirk));
        
        // Command Listeners
        twirk.addIrcListener(new CommandManagerListener(twirk));
        twirk.addIrcListener(new GenericCommandListener(twirk, twitchApi, streamerUser));
        //twirk.addIrcListener(new ModListener(twirk));
        twirk.addIrcListener(new LeaderboardListener(twirk, twitchApi));
        twirk.addIrcListener(new QuoteListener(twirk));
        twirk.addIrcListener(predsGuessListener);
        twirk.addIrcListener(new PredsManagerListener(twirk, twitchApi, predsGuessListener));
        twirk.addIrcListener(queueJoinListener);
        twirk.addIrcListener(new ScheduledMessageManagerListener(twirk));
        twirk.addIrcListener(new ViewerQueueManageListener(viewerQueueManager, queueJoinListener));
        twirk.addIrcListener(new WatchTimeListener(twirk, streamTracker));
        twirk.addIrcListener(new WrListener(twirk, twitchApi));
    
        // General Listeners
        twirk.addIrcListener(new ChatLoggerListener(chatLogger));
        twirk.addIrcListener(cloudListener);
        twirk.addIrcListener(new EmoteListener());
        twirk.addIrcListener(new LinkListener(twirk, twitchApi, twitter));
        twirk.addIrcListener(new PyramidListener(twirk));
        twirk.addIrcListener(socialScheduler.getListener());
        twirk.addIrcListener(new SubListener(twirk));
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
