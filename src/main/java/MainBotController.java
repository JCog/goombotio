import com.gikk.twirk.events.TwirkListener;
import com.github.twitch4j.helix.domain.User;
import database.DbManager;
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
import util.TwitchApi;

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
    private final DiscordListener discordListener;
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
    private final SubPointUpdater subPointUpdater;

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
        discordListener = new DiscordListener();
        discordBotController = new DiscordBotController(settings.getDiscordToken(), discordListener);
        chatLogger = new ChatLogger();
        twitchApi = new TwitchApi(
                chatLogger,
                settings.getTwitchStream(),
                settings.getTwitchUsername(),
                settings.getTwitchChannelAuthToken(),
                settings.getTwitchChannelClientId(),
                settings.isSilentMode()
        );
        botUser = twitchApi.getBotUser();
        if (botUser == null) {
            out.println("Error retrieving bot user");
            System.exit(1);
        }
        streamerUser = twitchApi.getStreamerUser();
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
        pubSub = new PubSub(
                twitchApi,
                dbManager,
                streamerUser.getId(),
                settings.getTwitchChannelAuthToken()
        );
        cloudListener = new CloudListener(twitchApi);
        streamTracker = new StreamTracker(
                dbManager,
                twitchApi,
                streamerUser,
                scheduler,
                cloudListener
        );
        scheduledMessageController = new ScheduledMessageController(
                dbManager,
                twitchApi,
                scheduler,
                streamerUser,
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
        viewerQueueManager = new ViewerQueueManager(twitchApi, dbManager);
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
        subPointUpdater = new SubPointUpdater(twitchApi, streamerUser, settings);
    }

    public synchronized void run() {
        scheduledMessageController.start();
        followLogger.start();
        addAllListeners();
        twirk.connect();
        streamTracker.start();
        minecraftWhitelistUpdater.start();
        subPointUpdater.start();
        pubSub.listenForBits().listenForChannelPoints();

        out.println("Goombotio is ready.");

        //main loop
        try {
            new ConsoleCommandListener(twitchApi, discordBotController).run();
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
        subPointUpdater.stop();
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
        twirk.addIrcListener(new BitWarResetListener(scheduler, twitchApi, dbManager));
        twirk.addIrcListener(new CommandManagerListener(scheduler, twirk, dbManager));
        twirk.addIrcListener(new GenericCommandListener(scheduler, twirk, dbManager, twitchApi, streamerUser));
        //twirk.addIrcListener(new ModListener(scheduler, twirk));
        twirk.addIrcListener(new LeaderboardListener(scheduler, dbManager, twitchApi, streamerUser));
        twirk.addIrcListener(new MinecraftListener(scheduler, twitchApi, dbManager, minecraftWhitelistUpdater));
        twirk.addIrcListener(new QuoteListener(scheduler, dbManager, twitchApi, streamerUser));
        twirk.addIrcListener(predsGuessListener);
        twirk.addIrcListener(new PredsManagerListener(
                scheduler, dbManager, twitchApi, discordBotController, predsGuessListener, streamerUser));
        twirk.addIrcListener(queueJoinListener);
        twirk.addIrcListener(new ScheduledMessageManagerListener(scheduler, twitchApi, dbManager));
        twirk.addIrcListener(new TattleListener(scheduler, dbManager, twitchApi));
        twirk.addIrcListener(new ViewerQueueManageListener(scheduler, viewerQueueManager, queueJoinListener));
        twirk.addIrcListener(new WatchTimeListener(scheduler, twitchApi, dbManager, streamTracker));
        twirk.addIrcListener(new WrListener(scheduler, twitchApi, streamerUser));

        // General Listeners
        twirk.addIrcListener(new ChatLoggerListener(chatLogger));
        twirk.addIrcListener(cloudListener);
        twirk.addIrcListener(new EmoteListener(dbManager));
        twirk.addIrcListener(new LinkListener(twitchApi, twitter, settings.getYoutubeApiKey()));
        twirk.addIrcListener(new PyramidListener(twitchApi));
        twirk.addIrcListener(scheduledMessageController.getListener());
        twirk.addIrcListener(new SubListener(twitchApi, scheduler));
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
