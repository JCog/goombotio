import database.DbManager;
import functions.*;
import listeners.channelpoints.DethroneListener;
import listeners.channelpoints.VipRaffleRewardListener;
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
import util.MessageExpressionParser;
import util.Settings;
import util.TwitchApi;

import java.util.NoSuchElementException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static java.lang.System.out;

public class MainBotController {
    private static final String DB_NAME = "goombotio";
    private static final int TIMER_THREAD_SIZE = 5;

    private final Settings settings;
    private final DbManager dbManager;
    private final ScheduledExecutorService scheduler;
    private final Twitter twitter;
    private final DiscordListener discordListener;
    private final DiscordBotController discordBotController;
    private final ChatLogger chatLogger;
    private final TwitchApi twitchApi;
    private final StreamTracker streamTracker;
    private final MessageExpressionParser messageExpressionParser;
    private final ScheduledMessageController scheduledMessageController;
    private final FollowLogger followLogger;
//    private final MinecraftWhitelistUpdater minecraftWhitelistUpdater;
    private final SubPointUpdater subPointUpdater;

    public MainBotController() {
        settings = new Settings();
        out.printf(
                "\nWrite permission: %s\nSilent Chat: %s\n\n",
                settings.hasWritePermission() ? "TRUE" : "FALSE",
                settings.isSilentMode() ? "TRUE" : "FALSE"
        );
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
                settings.getTwitchBotAuthToken(),
                settings.isSilentMode()
        );
        streamTracker = new StreamTracker(
                dbManager,
                twitchApi,
                scheduler
        );
        messageExpressionParser = new MessageExpressionParser(dbManager, twitchApi);
        scheduledMessageController = new ScheduledMessageController(
                dbManager,
                twitchApi,
                scheduler,
                messageExpressionParser
        );
        followLogger = new FollowLogger(
                dbManager,
                twitchApi,
                streamTracker,
                scheduler
        );
//        minecraftWhitelistUpdater = new MinecraftWhitelistUpdater(
//                dbManager,
//                twitchApi,
//                scheduler,
//                settings.getMinecraftServer(),
//                settings.getMinecraftUser(),
//                settings.getMinecraftPassword(),
//                settings.getMinecraftWhitelistLocation()
//        );
        subPointUpdater = new SubPointUpdater(twitchApi, settings, scheduler);
    }

    public synchronized void run(long startTime) {
        out.print("Initializing internal processes... ");
        followLogger.start();
        registerListeners();
        streamTracker.start();
//        minecraftWhitelistUpdater.start();
        out.println("success.");

        out.printf("Goombotio is ready. (~%ds start time)%n%n", (System.currentTimeMillis() - startTime) / 1000);

        //main loop
        try {
            new ConsoleCommandListener(twitchApi, discordBotController).run();
        } catch (NoSuchElementException nsee) {
            out.println("No console detected. Process must be killed manually.");
            try {
                this.wait();
            } catch (InterruptedException ie) {
                ie.printStackTrace();
            }
        }
    }

    public void closeAll() {
//        minecraftWhitelistUpdater.stop();
        streamTracker.stop();
        followLogger.stop();
        chatLogger.close();
        discordBotController.close();
        dbManager.closeDb();
        scheduler.shutdown();
    }

    private void registerListeners() {
        //setup
        PredsGuessListener predsGuessListener = new PredsGuessListener();

        // Command Listeners
        twitchApi.registerEventListener(new AdCommandListener(scheduler, twitchApi));
//        twitchApi.registerEventListener(new BitWarResetCommandListener(scheduler, twitchApi, dbManager));
        twitchApi.registerEventListener(new CommandManagerListener(scheduler, twitchApi, dbManager));
        twitchApi.registerEventListener(new GenericCommandListener(scheduler, messageExpressionParser, dbManager, twitchApi));
        twitchApi.registerEventListener(new LeaderboardListener(scheduler, dbManager, twitchApi));
//        twitchApi.registerEventListener(new MinecraftListener(scheduler, twitchApi, dbManager, minecraftWhitelistUpdater));
//        twitchApi.registerEventListener(new ModListener(scheduler, twitchApi));
        twitchApi.registerEventListener(new QuoteListener(scheduler, dbManager, twitchApi));
        twitchApi.registerEventListener(new PermanentVipListener(scheduler, twitchApi, dbManager));
        twitchApi.registerEventListener(new PredsManagerListener(scheduler, dbManager, twitchApi, discordBotController, predsGuessListener));
        twitchApi.registerEventListener(new RacetimeListener(scheduler, twitchApi));
        twitchApi.registerEventListener(new ScheduledMessageManagerListener(scheduler, twitchApi, dbManager));
        twitchApi.registerEventListener(new TattleListener(scheduler, dbManager, twitchApi));
        twitchApi.registerEventListener(new VipRaffleListener(scheduler, twitchApi, dbManager));
        twitchApi.registerEventListener(new WatchTimeListener(scheduler, twitchApi, dbManager, streamTracker));
        twitchApi.registerEventListener(new WrListener(scheduler, twitchApi));
        
        twitchApi.registerEventListener(predsGuessListener);
        
        // Channel Point Listeners
        twitchApi.registerEventListener(new DethroneListener(twitchApi, dbManager));
        twitchApi.registerEventListener(new VipRaffleRewardListener(twitchApi, dbManager));

        // General Listeners
        twitchApi.registerEventListener(new AdEventListener(twitchApi));
//        twitchApi.registerEventListener(new BitWarCheerListener(twitchApi, dbManager));
        twitchApi.registerEventListener(new ChatLoggerListener(chatLogger));
        twitchApi.registerEventListener(new CloudListener(twitchApi));
        twitchApi.registerEventListener(new EmoteListener(dbManager));
        twitchApi.registerEventListener(new LinkListener(twitchApi, twitter, settings.getYoutubeApiKey()));
        twitchApi.registerEventListener(new PyramidListener(twitchApi));
        twitchApi.registerEventListener(new RecentCheerListener(twitchApi));
        twitchApi.registerEventListener(new ShoutoutListener(twitchApi));
        twitchApi.registerEventListener(new SubListener(twitchApi));
        
        // Misc
        twitchApi.registerEventListener(scheduledMessageController);
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
