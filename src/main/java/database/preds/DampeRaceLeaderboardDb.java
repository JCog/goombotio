package database.preds;

import database.GbCollection;
import database.GbDatabase;
import org.bson.Document;

public class DampeRaceLeaderboardDb extends GbCollection {
    private static final String COLLECTION_NAME_KEY = "dampe_race";
    
    private static final String NAME_KEY = "name";
    private static final String WINS_KEY = "wins";
    
    public DampeRaceLeaderboardDb(GbDatabase gbDatabase) {
        super(gbDatabase);
    }
    
    public int getWinCount(String userId) {
        Document result = findFirstEquals(ID_KEY, userId);
        if (result == null) {
            return 0;
        } else {
            return result.getInteger(WINS_KEY);
        }
    }
    
    public void addWin(String userId, String displayName) {
        Document result = findFirstEquals(ID_KEY, userId);
        if (result == null) {
            Document document = new Document(ID_KEY, userId)
                    .append(NAME_KEY, displayName)
                    .append(WINS_KEY, 1);
            insertOne(document);
        } else {
            int newWins = result.getInteger(WINS_KEY) + 1;
            updateOne(userId, new Document(WINS_KEY, newWins));
        }
    }
    
    @Override
    protected String getCollectionName() {
        return COLLECTION_NAME_KEY;
    }
}
