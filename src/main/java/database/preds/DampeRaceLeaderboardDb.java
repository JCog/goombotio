package database.preds;

import com.mongodb.client.FindIterable;
import com.mongodb.client.model.Sorts;
import database.GbCollection;
import database.GbDatabase;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;

public class DampeRaceLeaderboardDb extends GbCollection {
    private static final String COLLECTION_NAME = "dampe_race";
    
    private static final String NAME_KEY = "name";
    private static final String WINS_KEY = "wins";
    
    public DampeRaceLeaderboardDb(GbDatabase gbDatabase) {
        super(gbDatabase, COLLECTION_NAME);
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
    
    public List<DampeRaceLbItem> getWinners() {
        FindIterable<Document> winnerDocs = findAll().sort(Sorts.descending(WINS_KEY));
        List<DampeRaceLbItem> winnerItems = new ArrayList<>();
        for (Document winnerDoc : winnerDocs) {
            winnerItems.add(new DampeRaceLbItem(
                    winnerDoc.getString(ID_KEY),
                    winnerDoc.getString(NAME_KEY),
                    winnerDoc.getInteger(WINS_KEY)
            ));
        }
        return winnerItems;
    }
    
    public static class DampeRaceLbItem {
    
        private final String userId;
        private final String displayName;
        private final int winCount;
    
        public DampeRaceLbItem(String userId, String displayName, int winCount) {
            this.userId = userId;
            this.displayName = displayName;
            this.winCount = winCount;
        }
        
        public String getUserId() {
            return userId;
        }
    
        public String getDisplayName() {
            return displayName;
        }
    
        public int getWinCount() {
            return winCount;
        }
    }
}
