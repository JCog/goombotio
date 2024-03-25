import database.DbManager;
import functions.*;
import listeners.TwitchEventListener;
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
import util.MessageExpressionParser;
import util.Settings;
import util.TwitchApi;

import java.util.NoSuchElementException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static java.lang.System.out;

public class MainBotController {
    private static final String DB_NAME = "goombotio";
    private static final int TIMER_THREAD_SIZE = 20;

    private final Settings settings;
    private final DbManager dbManager;
    private final ScheduledExecutorService scheduler;
    private final Twitter twitter;
    private final DiscordListener discordListener;
    private final DiscordBotController discordBotController;
    private final TwitchApi twitchApi;
    private final StreamTracker streamTracker;
    private final MessageExpressionParser messageExpressionParser;
    private final ScheduledMessageController scheduledMessageController;
    private final FollowLogger followLogger;
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
        twitchApi = new TwitchApi(
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
        subPointUpdater = new SubPointUpdater(twitchApi, settings, scheduler);
    }

    public synchronized void run(long startTime) {
        out.print("Initializing internal processes... ");
        registerListeners();
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
        streamTracker.stop();
        discordBotController.close();
        dbManager.closeDb();
        scheduler.shutdown();
    }

    private void registerListeners() {
        // BitWarResetCommandListener and MinecraftListener currently unused
        PredsGuessListener predsGuessListener = new PredsGuessListener();
        TwitchEventListener[] listeners = {
                // Setup
                predsGuessListener,
                
                // Commands
                new AdCommandListener(twitchApi),
                new CommandManagerListener(twitchApi, dbManager),
                new GenericCommandListener(messageExpressionParser, dbManager, twitchApi),
                new LeaderboardListener(dbManager, twitchApi),
                new QuoteListener(dbManager, twitchApi),
                new PermanentVipListener(twitchApi, dbManager),
                new PredsManagerListener(dbManager, twitchApi, discordBotController, predsGuessListener),
                new RacetimeListener(twitchApi),
                new ScheduledMessageManagerListener(twitchApi, dbManager),
                new TattleListener(dbManager, twitchApi),
                new VipRaffleListener(twitchApi, dbManager),
                new WatchTimeListener(twitchApi, dbManager, streamTracker),
                new WrListener(twitchApi),
                
                // Channel Points
                new DethroneListener(twitchApi, dbManager),
                new VipRaffleRewardListener(twitchApi, dbManager),
                
                // General
                new AdEventListener(twitchApi),
                new ChatLoggerListener(),
                new CloudListener(twitchApi),
                new EmoteListener(dbManager),
                new LinkListener(twitchApi, twitter, settings.getYoutubeApiKey()),
                new PyramidListener(twitchApi),
                new RecentCheerListener(twitchApi),
                new ShoutoutListener(twitchApi),
                new SubListener(twitchApi),
                
                // Misc
                scheduledMessageController,
        };
        for (TwitchEventListener listener : listeners) {
            twitchApi.registerEventListener(listener);
        }
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
