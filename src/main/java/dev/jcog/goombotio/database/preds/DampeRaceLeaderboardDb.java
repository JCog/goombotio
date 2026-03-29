package dev.jcog.goombotio.database.preds;

import dev.jcog.goombotio.database.GbDatabase;

public class DampeRaceLeaderboardDb extends PredsLeaderboardDbBase {
    private static final String COLLECTION_NAME = "preds_dampe_race";
    
    public DampeRaceLeaderboardDb(GbDatabase gbDatabase) {
        super(gbDatabase, COLLECTION_NAME);
    }
}
