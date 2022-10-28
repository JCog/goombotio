package database.emotes;

import database.GbDatabase;

public class SevenTvEmoteStatsDb extends EmoteStatsDb {
    private static final String COLLECTION_NAME_KEY = "seventvemotestats";

    public SevenTvEmoteStatsDb(GbDatabase gbDatabase) {
        super(gbDatabase);
    }

    @Override
    protected String getCollectionName() {
        return COLLECTION_NAME_KEY;
    }
}
