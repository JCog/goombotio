package database.preds;


import com.mongodb.client.model.Sorts;
import database.GbDatabase;
import org.bson.Document;

import java.util.ArrayList;

public class SpeedySpinLeaderboardDb extends PredsLeaderboardDb {
    private static final String COLLECTION_NAME = "speedyspin";

    public SpeedySpinLeaderboardDb(GbDatabase gbDatabase) {
        super(gbDatabase, COLLECTION_NAME);
    }
    
    public ArrayList<SpeedySpinItem> getAllSortedWins() {
        ArrayList<SpeedySpinItem> items = new ArrayList<>();
        for (Document doc : findAll().sort(Sorts.descending(WINS_KEY))) {
            if (doc.getInteger(WINS_KEY) > 0) {
                items.add(new SpeedySpinItem(
                        doc.getLong(ID_KEY),
                        doc.getString(NAME_KEY),
                        doc.getInteger(WINS_KEY),
                        doc.getInteger(POINTS_KEY)
                ));
            }
        }
        return items;
    }
    
    public ArrayList<SpeedySpinItem> getAllSortedPoints() {
        ArrayList<SpeedySpinItem> items = new ArrayList<>();
        for (Document doc : findAll().sort(Sorts.descending(POINTS_KEY))) {
            items.add(new SpeedySpinItem(
                    doc.getLong(ID_KEY),
                    doc.getString(NAME_KEY),
                    doc.getInteger(WINS_KEY),
                    doc.getInteger(POINTS_KEY)
            ));
        }
        return items;
    }
    
    public static class SpeedySpinItem {
        private final long userId;
        private final String displayName;
        private final int wins;
        private final int points;
    
        public SpeedySpinItem(long userId, String displayName, int wins, int points) {
            this.userId = userId;
            this.displayName = displayName;
            this.wins = wins;
            this.points = points;
        }
    
        public long getUserId() {
            return userId;
        }
    
        public String getDisplayName() {
            return displayName;
        }
    
        public int getWins() {
            return wins;
        }
    
        public int getPoints() {
            return points;
        }
    }
}
