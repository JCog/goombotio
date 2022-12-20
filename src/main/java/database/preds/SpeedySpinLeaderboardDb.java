package database.preds;


import database.GbDatabase;

public class SpeedySpinLeaderboardDb extends PredsLeaderboardDb {
    private static final String COLLECTION_NAME = "speedyspin";

    public SpeedySpinLeaderboardDb(GbDatabase gbDatabase) {
        super(gbDatabase, COLLECTION_NAME);
    }
}
