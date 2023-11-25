package database;

import database.emotes.*;
import database.misc.*;
import database.preds.BadgeShopLeaderboardDb;
import database.preds.BoosterHillLeaderboardDb;
import database.preds.DampeRaceLeaderboardDb;
import database.preds.PiantaSixLeaderboardDb;
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
    private final VipDb vipDb;
    private final QuoteDb quoteDb;
    private final SocialSchedulerDb socialSchedulerDb;
    private final StatsBlacklistDb statsBlacklistDb;
    private final TattleDb tattleDb;
    private final ViewerQueueDb viewerQueueDb;
    private final VipRaffleDb vipRaffleDb;

    // preds DBs
    private final DampeRaceLeaderboardDb dampeRaceLeaderboardDb;
    private final BadgeShopLeaderboardDb badgeShopLeaderboardDb;
    private final PiantaSixLeaderboardDb piantaSixLeaderboardDb;
    private final BoosterHillLeaderboardDb boosterHillLeaderboardDb;

    // stats DBs
    private final StreamStatsDb streamStatsDb;
    private final WatchTimeDb watchTimeDb;

    public DbManager(String host, Integer port, String dbName, String user, String password, boolean writePermission) {
        if (host == null || port == null || user == null || password == null) {
            System.out.printf("Establishing database connection to %s at localhost:27017... ", dbName);
            this.gbDatabase = new GbDatabase(dbName, writePermission);
        } else {
            System.out.printf("Establishing database connection to %s at %s:%d... ", dbName, host, port);
            this.gbDatabase = new GbDatabase(host, port, dbName, user, password, writePermission);
        }

        emoteStatsDb = new EmoteStatsDb(gbDatabase);
        ffzEmoteStatsDb = new FfzEmoteStatsDb(gbDatabase);
        sevenTvEmoteStatsDb = new SevenTvEmoteStatsDb(gbDatabase);
        bttvEmoteStatsDb = new BttvEmoteStatsDb(gbDatabase);

        bitWarDb = new BitWarDb(gbDatabase);
        commandDb = new CommandDb(gbDatabase);
        minecraftUserDb = new MinecraftUserDb(gbDatabase);
        quoteDb = new QuoteDb(gbDatabase);
        socialSchedulerDb = new SocialSchedulerDb(gbDatabase);
        statsBlacklistDb = new StatsBlacklistDb(gbDatabase);
        tattleDb = new TattleDb(gbDatabase);
        viewerQueueDb = new ViewerQueueDb(gbDatabase);
        vipDb = new VipDb(gbDatabase);
        vipRaffleDb = new VipRaffleDb(gbDatabase);
    
        dampeRaceLeaderboardDb = new DampeRaceLeaderboardDb(gbDatabase);
        badgeShopLeaderboardDb = new BadgeShopLeaderboardDb(gbDatabase);
        piantaSixLeaderboardDb = new PiantaSixLeaderboardDb(gbDatabase);
        boosterHillLeaderboardDb = new BoosterHillLeaderboardDb(gbDatabase);

        streamStatsDb = new StreamStatsDb(gbDatabase);
        watchTimeDb = new WatchTimeDb(gbDatabase);
        
        System.out.println("success.");
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

    public QuoteDb getQuoteDb() {
        return quoteDb;
    }

    public SocialSchedulerDb getSocialSchedulerDb() {
        return socialSchedulerDb;
    }
    
    public StatsBlacklistDb getStatsBlacklistDb() {
        return statsBlacklistDb;
    }

    public TattleDb getTattleDb() {
        return tattleDb;
    }

    public ViewerQueueDb getViewerQueueDb() {
        return viewerQueueDb;
    }
    
    public VipDb getVipDb() {
        return vipDb;
    }
    
    public VipRaffleDb getVipRaffleDb() {
        return vipRaffleDb;
    }
    
    public DampeRaceLeaderboardDb getDampeRaceLeaderboardDb() {
        return dampeRaceLeaderboardDb;
    }

    public BadgeShopLeaderboardDb getBadgeShopLeaderboardDb() {
        return badgeShopLeaderboardDb;
    }

    public PiantaSixLeaderboardDb getPiantaSixLeaderboardDb() {
        return piantaSixLeaderboardDb;
    }
    
    public BoosterHillLeaderboardDb getBoosterHillLeaderboardDb() {
        return boosterHillLeaderboardDb;
    }

    public StreamStatsDb getStreamStatsDb() {
        return streamStatsDb;
    }

    public WatchTimeDb getWatchTimeDb() {
        return watchTimeDb;
    }
}
