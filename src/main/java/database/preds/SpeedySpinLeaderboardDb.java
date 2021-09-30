package database.preds;


import database.GbDatabase;

public class SpeedySpinLeaderboardDb extends PredsLeaderboardDb {
    private static final String COLLECTION_NAME_KEY = "speedyspin";

    public SpeedySpinLeaderboardDb(GbDatabase gbDatabase) {
        super(gbDatabase);
    }

    @Override
    protected String getCollectionName() {
        return COLLECTION_NAME_KEY;
    }
}
