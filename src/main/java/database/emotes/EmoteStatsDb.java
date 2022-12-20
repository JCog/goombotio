package database.emotes;

import database.GbDatabase;

public class EmoteStatsDb extends EmoteStatsDbBase {
    private static final String COLLECTION_NAME = "emotestats";

    public EmoteStatsDb(GbDatabase gbDatabase) {
        super(gbDatabase, COLLECTION_NAME);
    }
}
