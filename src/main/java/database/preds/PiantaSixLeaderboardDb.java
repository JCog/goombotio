package database.preds;

import database.GbDatabase;

public class PiantaSixLeaderboardDb extends PredsLeaderboardDbBase {
    private static final String COLLECTION_NAME = "preds_pianta_six";

    public PiantaSixLeaderboardDb(GbDatabase gbDatabase) {
        super(gbDatabase, COLLECTION_NAME);
    }
}
