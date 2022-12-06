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
import util.TwitchApi;

import java.util.NoSuchElementException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

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
    private final PubSub pubSub;
    private final StreamTracker streamTracker;
    private final ScheduledMessageController scheduledMessageController;
    private final FollowLogger followLogger;
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
        pubSub = new PubSub(
                twitchApi,
                dbManager,
                streamerUser.getId(),
                settings.getTwitchChannelAuthToken()
        );
        streamTracker = new StreamTracker(
                dbManager,
                twitchApi,
                streamerUser,
                scheduler
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
        discordBotController.close();
        dbManager.closeDb();
        scheduler.shutdown();
    }

    private void addAllListeners() {
        //setup
        PredsGuessListener predsGuessListener = new PredsGuessListener();

        // Command Listeners
        twitchApi.registerEventListener(new BitWarResetListener(scheduler, twitchApi, dbManager));
        twitchApi.registerEventListener(new CommandManagerListener(scheduler, twitchApi, dbManager));
        twitchApi.registerEventListener(new GenericCommandListener(scheduler, dbManager, twitchApi, streamerUser));
        twitchApi.registerEventListener(new LeaderboardListener(scheduler, dbManager, twitchApi, streamerUser));
        twitchApi.registerEventListener(new MinecraftListener(scheduler, twitchApi, dbManager, minecraftWhitelistUpdater));
//        twitchApi.registerEventListener(new ModListener(scheduler, twitchApi));
        twitchApi.registerEventListener(new QuoteListener(scheduler, dbManager, twitchApi, streamerUser));
        twitchApi.registerEventListener(new PredsManagerListener(
                scheduler, dbManager, twitchApi, discordBotController, predsGuessListener, streamerUser));
        twitchApi.registerEventListener(new ScheduledMessageManagerListener(scheduler, twitchApi, dbManager));
        twitchApi.registerEventListener(new TattleListener(scheduler, dbManager, twitchApi));
        twitchApi.registerEventListener(new WatchTimeListener(scheduler, twitchApi, dbManager, streamTracker));
        twitchApi.registerEventListener(new WrListener(scheduler, twitchApi, streamerUser));
        
        twitchApi.registerEventListener(predsGuessListener);

        // General Listeners
        twitchApi.registerEventListener(new ChatLoggerListener(chatLogger));
        twitchApi.registerEventListener(new CloudListener(twitchApi));
        twitchApi.registerEventListener(new EmoteListener(dbManager));
        twitchApi.registerEventListener(new LinkListener(twitchApi, twitter, settings.getYoutubeApiKey()));
        twitchApi.registerEventListener(new PyramidListener(twitchApi));
        twitchApi.registerEventListener(scheduledMessageController.getListener());
        twitchApi.registerEventListener(new SubListener(twitchApi));
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
