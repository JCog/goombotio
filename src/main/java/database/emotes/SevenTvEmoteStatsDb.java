package database.emotes;

import database.GbDatabase;

public class SevenTvEmoteStatsDb extends EmoteStatsDbBase {
    private static final String COLLECTION_NAME = "seventvemotestats";

    public SevenTvEmoteStatsDb(GbDatabase gbDatabase) {
        super(gbDatabase, COLLECTION_NAME);
    }
}
