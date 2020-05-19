package Database.Preds;

import com.mongodb.client.MongoCollection;
import org.bson.Document;

public class SunshineTimerLeaderboard extends PredsLeaderboard {
    private static final String COLLECTION_NAME_KEY = "sunshinetimer";
    
    private static SunshineTimerLeaderboard instance = null;
    
    private SunshineTimerLeaderboard() {
        super();
    }
    
    public static SunshineTimerLeaderboard getInstance() {
        if (instance == null) {
            instance = new SunshineTimerLeaderboard();
        }
        return instance;
    }
    
    @Override
    protected MongoCollection<Document> setCollection() {
        return goombotioDb.getCollection(COLLECTION_NAME_KEY);
    }
}
