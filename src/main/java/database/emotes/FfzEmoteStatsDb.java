package database.emotes;

import database.GbDatabase;

public class FfzEmoteStatsDb extends EmoteStatsDb {
    private static final String COLLECTION_NAME_KEY = "ffzemotestats";

    public FfzEmoteStatsDb(GbDatabase gbDatabase) {
        super(gbDatabase);
    }

    @Override
    protected String getCollectionName() {
        return COLLECTION_NAME_KEY;
    }
}
