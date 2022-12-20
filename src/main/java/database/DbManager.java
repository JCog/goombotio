package database;

import database.emotes.*;
import database.misc.*;
import database.preds.DampeRaceLeaderboardDb;
import database.preds.SpeedySpinLeaderboardDb;
import database.preds.SunshineTimerLeaderboardDb;
import database.stats.StreamStatsDb;
import database.stats.WatchTimeDb;

public class DbManager {
    private final GbDatabase gbDatabase;
    
    // emote DBs
    private final BttvEmoteStatsDb bttvEmoteStatsDb;
    private final EmoteStatsDbBase emoteStatsDb;
    private final FfzEmoteStatsDb ffzEmoteStatsDb;
    private final SevenTvEmoteStatsDb sevenTvEmoteStatsDb;

    // misc DBs
    private final BitWarDb bitWarDb;
    private final CommandDb commandDb;
    private final MinecraftUserDb minecraftUserDb;
    private final PermanentVipsDb permanentVipsDb;
    private final QuoteDb quoteDb;
    private final SocialSchedulerDb socialSchedulerDb;
    private final TattleDb tattleDb;
    private final ViewerQueueDb viewerQueueDb;
    private final VipRaffleDb vipRaffleDb;

    // preds DBs
    private final DampeRaceLeaderboardDb dampeRaceLeaderboardDb;
    private final SpeedySpinLeaderboardDb speedySpinLeaderboardDb;
    private final SunshineTimerLeaderboardDb sunshineTimerLeaderboardDb;

    // stats DBs
    private final StreamStatsDb streamStatsDb;
    private final WatchTimeDb watchTimeDb;

    public DbManager(String host, Integer port, String dbName, String user, String password, boolean writePermission) {
        if (host == null || port == null || user == null || password == null) {
            this.gbDatabase = new GbDatabase(dbName, writePermission);
            System.out.printf("Database connection to %s at localhost:27017 successful.%n", dbName);
        } else {
            this.gbDatabase = new GbDatabase(host, port, dbName, user, password, writePermission);
            System.out.printf("Database connection to %s at %s:%d successful.%n", dbName, host, port);
        }

        emoteStatsDb = new EmoteStatsDb(gbDatabase);
        ffzEmoteStatsDb = new FfzEmoteStatsDb(gbDatabase);
        sevenTvEmoteStatsDb = new SevenTvEmoteStatsDb(gbDatabase);
        bttvEmoteStatsDb = new BttvEmoteStatsDb(gbDatabase);

        bitWarDb = new BitWarDb(gbDatabase);
        commandDb = new CommandDb(gbDatabase);
        minecraftUserDb = new MinecraftUserDb(gbDatabase);
        permanentVipsDb = new PermanentVipsDb(gbDatabase);
        quoteDb = new QuoteDb(gbDatabase);
        socialSchedulerDb = new SocialSchedulerDb(gbDatabase);
        tattleDb = new TattleDb(gbDatabase);
        viewerQueueDb = new ViewerQueueDb(gbDatabase);
        vipRaffleDb = new VipRaffleDb(gbDatabase);
    
        dampeRaceLeaderboardDb = new DampeRaceLeaderboardDb(gbDatabase);
        speedySpinLeaderboardDb = new SpeedySpinLeaderboardDb(gbDatabase);
        sunshineTimerLeaderboardDb = new SunshineTimerLeaderboardDb(gbDatabase);

        streamStatsDb = new StreamStatsDb(gbDatabase);
        watchTimeDb = new WatchTimeDb(gbDatabase);
    }

    public void closeDb() {
        gbDatabase.close();
    }

    public EmoteStatsDbBase getEmoteStatsDb() {
        return emoteStatsDb;
    }

    public FfzEmoteStatsDb getFfzEmoteStatsDb() {
        return ffzEmoteStatsDb;
    }
    
    public SevenTvEmoteStatsDb getSevenTvEmoteStatsDb() {
        return sevenTvEmoteStatsDb;
    }
    
    public BttvEmoteStatsDb getBttvEmoteStatsDb() {
        return bttvEmoteStatsDb;
    }

    public BitWarDb getBitWarDb() {
        return bitWarDb;
    }

    public CommandDb getCommandDb() {
        return commandDb;
    }

    public MinecraftUserDb getMinecraftUserDb() {
        return minecraftUserDb;
    }
    
    public PermanentVipsDb getPermanentVipsDb() {
        return permanentVipsDb;
    }

    public QuoteDb getQuoteDb() {
        return quoteDb;
    }

    public SocialSchedulerDb getSocialSchedulerDb() {
        return socialSchedulerDb;
    }

    public TattleDb getTattleDb() {
        return tattleDb;
    }

    public ViewerQueueDb getViewerQueueDb() {
        return viewerQueueDb;
    }
    
    public VipRaffleDb getVipRaffleDb() {
        return vipRaffleDb;
    }
    
    public DampeRaceLeaderboardDb getDampeRaceLeaderboardDb() {
        return dampeRaceLeaderboardDb;
    }

    public SpeedySpinLeaderboardDb getSpeedySpinLeaderboardDb() {
        return speedySpinLeaderboardDb;
    }

    public SunshineTimerLeaderboardDb getSunshineTimerLeaderboardDb() {
        return sunshineTimerLeaderboardDb;
    }

    public StreamStatsDb getStreamStatsDb() {
        return streamStatsDb;
    }

    public WatchTimeDb getWatchTimeDb() {
        return watchTimeDb;
    }
}
