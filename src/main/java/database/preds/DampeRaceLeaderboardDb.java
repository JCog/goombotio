package database.preds;

import database.GbDatabase;

public class DampeRaceLeaderboardDb extends PredsLeaderboardDbBase {
    private static final String COLLECTION_NAME = "preds_dampe_race";
    
    public DampeRaceLeaderboardDb(GbDatabase gbDatabase) {
        super(gbDatabase, COLLECTION_NAME);
    }
}
