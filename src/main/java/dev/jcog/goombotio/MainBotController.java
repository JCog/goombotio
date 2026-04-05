package dev.jcog.goombotio;

import dev.jcog.goombotio.api.ApiManager;
import dev.jcog.goombotio.database.DbManager;
import dev.jcog.goombotio.functions.*;
import dev.jcog.goombotio.listeners.TwitchEventListener;
import dev.jcog.goombotio.listeners.channelpoints.DethroneListener;
import dev.jcog.goombotio.listeners.channelpoints.VipRaffleRewardListener;
import dev.jcog.goombotio.listeners.commands.*;
import dev.jcog.goombotio.listeners.commands.preds.LeaderboardListener;
import dev.jcog.goombotio.listeners.commands.preds.PredsGuessListener;
import dev.jcog.goombotio.listeners.commands.preds.PredsManagerListener;
import dev.jcog.goombotio.listeners.commands.quotes.QuoteListener;
import dev.jcog.goombotio.listeners.events.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import twitter4j.Twitter;
import dev.jcog.goombotio.util.CommonUtils;
import dev.jcog.goombotio.util.MessageExpressionParser;
import dev.jcog.goombotio.util.Settings;
import dev.jcog.goombotio.util.TwitchApi;

import java.util.NoSuchElementException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class MainBotController {
    private static final Logger log = LoggerFactory.getLogger(MainBotController.class);
    private static final String DB_NAME = "goombotio";
    private static final int TIMER_THREAD_SIZE = 20;

    private final DbManager dbManager;
    private final TwitchApi twitchApi;
    private final DiscordBotController discordBotController;
    private final ScheduledExecutorService scheduler;
    private final CommonUtils commonUtils;
    private final Twitter twitter;
    private final String youtubeApiKey;
    private final StreamTracker streamTracker;

    public MainBotController() {
        Settings settings = new Settings();
        log.info("Write permission: {}", settings.hasWritePermission() ? "TRUE" : "FALSE");
        log.info("Silent Chat: {}", settings.isSilentMode() ? "TRUE" : "FALSE");
        dbManager = new DbManager(
                settings.getDbHost(),
                settings.getDbPort(),
                DB_NAME,
                settings.getDbUser(),
                settings.getDbPassword(),
                settings.hasWritePermission()
        );
        twitchApi = new TwitchApi(
                settings.getTwitchStream(),
                settings.getTwitchUsername(),
                settings.getTwitchChannelAuthToken(),
                settings.getTwitchChannelClientId(),
                settings.getTwitchBotAuthToken(),
                settings.isSilentMode()
        );
        discordBotController = new DiscordBotController(settings.getDiscordToken(), new DiscordListener());
        scheduler = Executors.newScheduledThreadPool(TIMER_THREAD_SIZE);
        commonUtils = new CommonUtils(twitchApi, dbManager, discordBotController, new ApiManager(), scheduler);
        
        twitter = Twitter.newBuilder()
                .oAuthConsumer(settings.getTwitterConsumerKey(), settings.getTwitterConsumerSecret())
                .oAuthAccessToken(settings.getTwitterAccessToken(), settings.getTwitterAccessTokenSecret())
                .build();
        youtubeApiKey = settings.getYoutubeApiKey();
        streamTracker = new StreamTracker(commonUtils);
        
        new FollowLogger(commonUtils, streamTracker);
        new SubPointUpdater(commonUtils, settings.getSubCountFormat());
        new Heartbeat(commonUtils);
    }

    public synchronized void run(long startTime) {
        log.info("Initializing internal processes... ");
        registerListeners();
        log.info("Internal processes initialized.");

        log.info("Goombotio is ready. (~{}s start time)", (System.currentTimeMillis() - startTime) / 1000);
        twitchApi.channelMessage("Goombotio started.");

        //main loop
        try {
            new ConsoleCommandListener(twitchApi, discordBotController).run();
        } catch (NoSuchElementException nsee) {
            log.warn("No console detected. Process must be killed manually.");
            try {
                this.wait();
            } catch (InterruptedException e) {
                log.error(e.getMessage());
            }
        }
    }

    public void closeAll() {
        twitchApi.channelMessage("Goombotio shutting down...");
        streamTracker.stop();
        discordBotController.close();
        dbManager.closeDb();
        scheduler.shutdown();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }
        twitchApi.close();
    }

    private void registerListeners() {
        MessageExpressionParser messageExpressionParser = new MessageExpressionParser(commonUtils);
        
        // BitWarResetCommandListener currently unused
        PredsGuessListener predsGuessListener = new PredsGuessListener();
        TwitchEventListener[] listeners = {
                // Setup
                predsGuessListener,
                
                // Commands
                new AdCommandListener(commonUtils),
                new CommandManagerListener(commonUtils),
                new GenericCommandListener(commonUtils, messageExpressionParser),
                new LeaderboardListener(commonUtils),
                new MuteCommandListener(commonUtils),
                new QuoteListener(commonUtils),
                new PermanentVipListener(commonUtils),
                new PredsManagerListener(commonUtils, predsGuessListener),
                new RacetimeListener(commonUtils),
                new ScheduledMessageManagerListener(commonUtils),
                new TattleListener(commonUtils),
                new VipRaffleListener(commonUtils),
                new WatchTimeListener(commonUtils, streamTracker),
                new WrListener(commonUtils),
                new YoutubeLinkListener(commonUtils),
                
                // Channel Points
                new DethroneListener(commonUtils),
                new VipRaffleRewardListener(commonUtils),
                
                // General
                new AdEventListener(commonUtils),
                new ChatLoggerListener(),
                new ChatModerationListener(commonUtils),
                new CloudListener(commonUtils),
                new EmoteListener(commonUtils),
                new HypeTrainEventListener(commonUtils),
                new LinkListener(commonUtils, twitter, youtubeApiKey),
                new PyramidListener(commonUtils),
                new RecentCheerListener(commonUtils),
                new ScheduledMessageController(commonUtils, messageExpressionParser),
                new ShoutoutListener(commonUtils),
                new SubListener(commonUtils),
        };
        for (TwitchEventListener listener : listeners) {
            twitchApi.registerEventListener(listener);
        }
    }
}
