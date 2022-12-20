package database.preds;


import database.GbDatabase;

public class SunshineTimerLeaderboardDb extends PredsLeaderboardDb {
    private static final String COLLECTION_NAME = "sunshinetimer";

    public SunshineTimerLeaderboardDb(GbDatabase gbDatabase) {
        super(gbDatabase, COLLECTION_NAME);
    }
}
