package database.preds;

import database.GbDatabase;

public class BoosterHillLeaderboardDb extends PredsLeaderboardDbBase {
    private static final String COLLECTION_NAME = "preds_booster_hill";

    public BoosterHillLeaderboardDb(GbDatabase gbDatabase) {
        super(gbDatabase, COLLECTION_NAME);
    }
}
