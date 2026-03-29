package dev.jcog.goombotio.database.preds;

import dev.jcog.goombotio.database.GbDatabase;

public class BoosterHillLeaderboardDb extends PredsLeaderboardDbBase {
    private static final String COLLECTION_NAME = "preds_booster_hill";

    public BoosterHillLeaderboardDb(GbDatabase gbDatabase) {
        super(gbDatabase, COLLECTION_NAME);
    }
}
