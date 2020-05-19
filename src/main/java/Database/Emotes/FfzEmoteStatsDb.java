package Database.Emotes;

import com.mongodb.client.MongoCollection;
import org.bson.Document;

public class FfzEmoteStatsDb extends EmoteStatsDb {
    private static final String COLLECTION_NAME_KEY = "ffzemotestats";
    
    private static FfzEmoteStatsDb instance = null;
    
    private FfzEmoteStatsDb() {
        super();
    }
    
    public static FfzEmoteStatsDb getInstance() {
        if (instance == null) {
            instance = new FfzEmoteStatsDb();
        }
        return instance;
    }
    @Override
    protected MongoCollection<Document> setCollection() {
        return goombotioDb.getCollection(COLLECTION_NAME_KEY);
    }
}
