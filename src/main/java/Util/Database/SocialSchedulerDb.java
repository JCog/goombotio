package Util.Database;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import org.bson.Document;

import java.util.ArrayList;

import static com.mongodb.client.model.Filters.eq;

public class SocialSchedulerDb extends CollectionBase {
    
    private static SocialSchedulerDb instance = null;
    
    private final String COLLECTION_NAME = "socialscheduler";
    private final String ID_KEY = "_id";
    private final String MESSAGE_KEY = "message";
    
    private SocialSchedulerDb() {
        super();
    }
    
    public static SocialSchedulerDb getInstance() {
        if (instance == null) {
            instance = new SocialSchedulerDb();
        }
        return instance;
    }
    
    @Override
    protected MongoCollection<Document> setCollection() {
        return goombotioDb.getCollection(COLLECTION_NAME);
    }
    
    public String getMessage(String id) {
        Document result = find(eq(ID_KEY, id)).first();
        if (result != null) {
            return result.getString(MESSAGE_KEY);
        }
        return null;
    }
    
    public ArrayList<String> getAllMessages() {
        MongoCursor<Document> result = find().iterator();
        ArrayList<String> messages = new ArrayList<>();
        
        while (result.hasNext()) {
            Document doc = result.next();
            messages.add(doc.getString(MESSAGE_KEY));
        }
        return messages;
    }
}
