package database.emotes;

import database.GbDatabase;

public class BttvEmoteStatsDb extends EmoteStatsDbBase {
    private static final String COLLECTION_NAME = "bttvemotestats";

    public BttvEmoteStatsDb(GbDatabase gbDatabase) {
        super(gbDatabase, COLLECTION_NAME);
    }
}
