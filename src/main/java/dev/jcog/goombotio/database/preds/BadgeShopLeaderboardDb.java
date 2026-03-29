package dev.jcog.goombotio.database.preds;

import dev.jcog.goombotio.database.GbDatabase;

public class BadgeShopLeaderboardDb extends PredsLeaderboardDbBase {
    private static final String COLLECTION_NAME = "preds_badge_shop";

    public BadgeShopLeaderboardDb(GbDatabase gbDatabase) {
        super(gbDatabase, COLLECTION_NAME);
    }
}
