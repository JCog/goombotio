import api.ApiManager;
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
import util.CommonUtils;
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
        out.print("Initializing internal processes... ");
        registerListeners();
        out.println("success.");

        out.printf("Goombotio is ready. (~%ds start time)%n%n", (System.currentTimeMillis() - startTime) / 1000);
        twitchApi.channelMessage("Goombotio started.");

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
        twitchApi.channelMessage("Goombotio shutting down...");
        streamTracker.stop();
        discordBotController.close();
        dbManager.closeDb();
        scheduler.shutdown();
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
                
                // Channel Points
                new DethroneListener(commonUtils),
                new VipRaffleRewardListener(commonUtils),
                
                // General
                new AdEventListener(commonUtils),
                new ChatLoggerListener(),
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
