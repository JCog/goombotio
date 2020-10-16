import com.gikk.twirk.events.TwirkListener;
import com.github.twitch4j.helix.domain.User;
import com.jcog.utils.TwitchApi;
import com.jcog.utils.database.DbManager;
import functions.*;
import listeners.commands.*;
import listeners.commands.preds.LeaderboardListener;
import listeners.commands.preds.PredsGuessListener;
import listeners.commands.preds.PredsManagerListener;
import listeners.events.*;
import twitter4j.Twitter;
import twitter4j.TwitterFactory;
import twitter4j.conf.ConfigurationBuilder;
import util.ChatLogger;
import util.Settings;
import util.TwirkInterface;

import java.util.Calendar;
import java.util.NoSuchElementException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static java.lang.System.out;

public class MainBotController {
    private static final String DB_NAME = "goombotio";
    private static final int SOCIAL_INTERVAL_LENGTH = 20;
    private static final int TIMER_THREAD_SIZE = 5;

    private final DbManager dbManager = new DbManager(
            Settings.getDbHost(),
            Settings.getDbPort(),
            DB_NAME,
            Settings.getDbUser(),
            Settings.getDbPassword(),
            Settings.hasWritePermission()
    );
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(TIMER_THREAD_SIZE);
    private final Twitter twitter = getTwitterInstance();
    private final DiscordBotController discordBotController = DiscordBotController.getInstance();
    private final ChatLogger chatLogger = new ChatLogger();
    private final TwitchApi twitchApi = new TwitchApi(
            Settings.getTwitchStream(),
            Settings.getTwitchChannelAuthToken(),
            Settings.getTwitchChannelClientId()
    );
    private final User botUser = twitchApi.getUserByUsername(Settings.getTwitchUsername());
    private final User streamerUser = twitchApi.getUserByUsername(Settings.getTwitchStream());
    private final TwirkInterface twirk = new TwirkInterface(chatLogger, botUser);
    private final CloudListener cloudListener = new CloudListener(twirk);
    private final StreamTracker streamTracker = new StreamTracker(
            twirk,
            dbManager,
            twitchApi,
            streamerUser,
            scheduler,
            cloudListener
    );
    private final ScheduledMessageController scheduledMessageController = new ScheduledMessageController(
            twirk,
            dbManager,
            twitchApi,
            scheduler,
            botUser,
            SOCIAL_INTERVAL_LENGTH
    );
    private final FollowLogger followLogger = new FollowLogger(
            dbManager,
            twitchApi,
            streamTracker,
            streamerUser,
            scheduler
    );
    private final ViewerQueueManager viewerQueueManager = new ViewerQueueManager(twirk, dbManager);
    private final MinecraftWhitelistUpdater minecraftWhitelistUpdater = new MinecraftWhitelistUpdater(
            dbManager,
            twitchApi,
            streamerUser,
            scheduler
    );

    public synchronized void run() {
        discordBotController.init();
        scheduledMessageController.start();
        followLogger.start();
        addAllListeners();
        twirk.connect();
        streamTracker.start();
        minecraftWhitelistUpdater.start();

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
        minecraftWhitelistUpdater.stop();
        streamTracker.stop();
        scheduledMessageController.stop();
        followLogger.stop();
        chatLogger.close();
        twirk.close();
        discordBotController.close();
        dbManager.closeDb();
        scheduler.shutdown();
    }

    private void addAllListeners() {
        //setup
        PredsGuessListener predsGuessListener = new PredsGuessListener();
        ViewerQueueJoinListener queueJoinListener = new ViewerQueueJoinListener(scheduler, viewerQueueManager);

        //connection handling
        twirk.addIrcListener(getOnDisconnectListener(twirk));

        // Command Listeners
        twirk.addIrcListener(new BitWarResetListener(scheduler, twirk, dbManager));
        twirk.addIrcListener(new CommandManagerListener(scheduler, twirk, dbManager));
        twirk.addIrcListener(new GenericCommandListener(scheduler, twirk, dbManager, twitchApi, streamerUser));
        //twirk.addIrcListener(new ModListener(scheduler, twirk));
        twirk.addIrcListener(new LeaderboardListener(scheduler, twirk, dbManager, twitchApi));
        twirk.addIrcListener(new MinecraftListener(scheduler, twirk, dbManager));
        twirk.addIrcListener(new QuoteListener(scheduler, twirk, dbManager));
        twirk.addIrcListener(predsGuessListener);
        twirk.addIrcListener(new PredsManagerListener(scheduler, twirk, dbManager, twitchApi, predsGuessListener));
        twirk.addIrcListener(queueJoinListener);
        twirk.addIrcListener(new ScheduledMessageManagerListener(scheduler, twirk, dbManager));
        twirk.addIrcListener(new ViewerQueueManageListener(scheduler, viewerQueueManager, queueJoinListener));
        twirk.addIrcListener(new WatchTimeListener(scheduler, twirk, dbManager, streamTracker));
        twirk.addIrcListener(new WrListener(scheduler, twirk, twitchApi));

        // General Listeners
        twirk.addIrcListener(new BitListener(twirk, dbManager));
        twirk.addIrcListener(new ChatLoggerListener(chatLogger));
        twirk.addIrcListener(cloudListener);
        twirk.addIrcListener(new EmoteListener(dbManager));
        twirk.addIrcListener(new LinkListener(twirk, twitchApi, twitter));
        twirk.addIrcListener(new PyramidListener(twirk));
        twirk.addIrcListener(scheduledMessageController.getListener());
        twirk.addIrcListener(new SubListener(twirk, scheduler));
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
                    }
                    catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } while (!twirk.connect());
            }
        };
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
