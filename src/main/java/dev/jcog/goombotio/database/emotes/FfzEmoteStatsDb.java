package dev.jcog.goombotio.database.emotes;

import dev.jcog.goombotio.database.GbDatabase;

public class FfzEmoteStatsDb extends EmoteStatsDbBase {
    private static final String COLLECTION_NAME = "ffzemotestats";

    public FfzEmoteStatsDb(GbDatabase gbDatabase) {
        super(gbDatabase, COLLECTION_NAME);
    }
}
