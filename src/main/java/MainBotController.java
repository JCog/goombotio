import com.gikk.twirk.events.TwirkListener;
import com.github.twitch4j.helix.domain.User;
import com.jcog.utils.TwitchApi;
import com.jcog.utils.database.DbManager;
import functions.*;
import listeners.commands.*;
import listeners.commands.preds.LeaderboardListener;
import listeners.commands.preds.PredsGuessListener;
import listeners.commands.preds.PredsManagerListener;
import listeners.commands.quotes.QuoteListener;
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

    private final Settings settings;
    private final DbManager dbManager;
    private final ScheduledExecutorService scheduler;
    private final Twitter twitter;
    private final DiscordBotController discordBotController;
    private final ChatLogger chatLogger;
    private final TwitchApi twitchApi;
    private final User botUser;
    private final User streamerUser;
    private final TwirkInterface twirk;
    private final PubSub pubSub;
    private final CloudListener cloudListener;
    private final StreamTracker streamTracker;
    private final ScheduledMessageController scheduledMessageController;
    private final FollowLogger followLogger;
    private final ViewerQueueManager viewerQueueManager;
    private final MinecraftWhitelistUpdater minecraftWhitelistUpdater;

    public MainBotController() {
        settings = new Settings();
        dbManager = new DbManager(
                settings.getDbHost(),
                settings.getDbPort(),
                DB_NAME,
                settings.getDbUser(),
                settings.getDbPassword(),
                settings.hasWritePermission()
        );
        scheduler = Executors.newScheduledThreadPool(TIMER_THREAD_SIZE);
        twitter = getTwitterInstance();
        discordBotController = DiscordBotController.getInstance();
        chatLogger = new ChatLogger();
        twitchApi = new TwitchApi(
                settings.getTwitchStream(),
                settings.getTwitchChannelAuthToken(),
                settings.getTwitchChannelClientId()
        );
        botUser = twitchApi.getUserByUsername(settings.getTwitchUsername());
        if (botUser == null) {
            out.println("Error retrieving bot user");
            System.exit(1);
        }
        streamerUser = twitchApi.getUserByUsername(settings.getTwitchStream());
        if (streamerUser == null) {
            out.println("Error retrieving streamer user");
            System.exit(1);
        }
        twirk = new TwirkInterface(
                chatLogger,
                botUser,
                settings.getTwitchStream(),
                settings.getTwitchUsername(),
                settings.getTwitchBotOauth(),
                settings.isSilentMode(),
                settings.isVerboseLogging()
        );
        pubSub = (PubSub) new PubSub(
                twirk,
                dbManager,
                streamerUser.getId(),
                settings.getTwitchChannelAuthToken()
        ).listenForBits();
        cloudListener = new CloudListener(twirk);
        streamTracker = new StreamTracker(
                twirk,
                dbManager,
                twitchApi,
                streamerUser,
                scheduler,
                cloudListener
        );
        scheduledMessageController = new ScheduledMessageController(
                twirk,
                dbManager,
                twitchApi,
                scheduler,
                botUser,
                SOCIAL_INTERVAL_LENGTH
        );
        followLogger = new FollowLogger(
                dbManager,
                twitchApi,
                streamTracker,
                streamerUser,
                scheduler
        );
        viewerQueueManager = new ViewerQueueManager(twirk, dbManager);
        minecraftWhitelistUpdater = new MinecraftWhitelistUpdater(
                dbManager,
                twitchApi,
                streamerUser,
                scheduler,
                settings.getMinecraftServer(),
                settings.getMinecraftUser(),
                settings.getMinecraftPassword(),
                settings.getMinecraftWhitelistLocation()
        );
    }

    public synchronized void run() {
        discordBotController.init(settings.getDiscordToken());
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
        pubSub.close();
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
        twirk.addIrcListener(new MinecraftListener(scheduler, twirk, dbManager, minecraftWhitelistUpdater));
        twirk.addIrcListener(new QuoteListener(scheduler, twirk, dbManager, twitchApi));
        twirk.addIrcListener(predsGuessListener);
        twirk.addIrcListener(new PredsManagerListener(scheduler, twirk, dbManager, twitchApi, predsGuessListener));
        twirk.addIrcListener(queueJoinListener);
        twirk.addIrcListener(new ScheduledMessageManagerListener(scheduler, twirk, dbManager));
        twirk.addIrcListener(new TattleListener(scheduler, dbManager, twirk, twitchApi));
        twirk.addIrcListener(new ViewerQueueManageListener(scheduler, viewerQueueManager, queueJoinListener));
        twirk.addIrcListener(new WatchTimeListener(scheduler, twirk, dbManager, streamTracker));
        twirk.addIrcListener(new WrListener(scheduler, twirk, twitchApi));

        // General Listeners
        twirk.addIrcListener(new ChatLoggerListener(chatLogger));
        twirk.addIrcListener(cloudListener);
        twirk.addIrcListener(new EmoteListener(dbManager));
        twirk.addIrcListener(new LinkListener(twirk, twitchApi, twitter, settings.getYoutubeApiKey()));
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
                    out.printf(
                            "%02d:%02d:%02d - Trying to connect again in 10 seconds%n",
                            hour,
                            minute,
                            second
                    );
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
                .setOAuthConsumerKey(settings.getTwitterConsumerKey())
                .setOAuthConsumerSecret(settings.getTwitterConsumerSecret())
                .setOAuthAccessToken(settings.getTwitterAccessToken())
                .setOAuthAccessTokenSecret(settings.getTwitterAccessTokenSecret());
        TwitterFactory tf = new TwitterFactory(cb.build());
        return tf.getInstance();
    }
}
