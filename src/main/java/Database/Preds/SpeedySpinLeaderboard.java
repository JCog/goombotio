package Database.Preds;

import com.mongodb.client.MongoCollection;
import org.bson.Document;

public class SpeedySpinLeaderboard extends PredsLeaderboard {
    private static final String COLLECTION_NAME_KEY = "speedyspin";
    
    private static SpeedySpinLeaderboard instance = null;
    
    private SpeedySpinLeaderboard() {
        super();
    }
    
    public static SpeedySpinLeaderboard getInstance() {
        if (instance == null) {
            instance = new SpeedySpinLeaderboard();
        }
        return instance;
    }
    
    @Override
    protected MongoCollection<Document> setCollection() {
        return goombotioDb.getCollection(COLLECTION_NAME_KEY);
    }
}
