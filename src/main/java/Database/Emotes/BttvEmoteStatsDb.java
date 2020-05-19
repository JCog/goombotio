package Database.Emotes;

import com.mongodb.client.MongoCollection;
import org.bson.Document;

public class BttvEmoteStatsDb extends EmoteStatsDb {
    private static final String COLLECTION_NAME_KEY = "bttvemotestats";
    
    private static BttvEmoteStatsDb instance = null;
    
    private BttvEmoteStatsDb() {
        super();
    }
    
    public static BttvEmoteStatsDb getInstance() {
        if (instance == null) {
            instance = new BttvEmoteStatsDb();
        }
        return instance;
    }
    @Override
    protected MongoCollection<Document> setCollection() {
        return goombotioDb.getCollection(COLLECTION_NAME_KEY);
    }
}
