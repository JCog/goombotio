package Database.Misc;

import Database.CollectionBase;
import Database.Entries.ScheduledMessage;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import org.bson.Document;

import java.util.ArrayList;

public class SocialSchedulerDb extends CollectionBase {
    
    private static final String COLLECTION_NAME = "socialscheduler";
    private static final String MESSAGE_KEY = "message";
    private static final String WEIGHT_KEY = "weight";
    
    private static SocialSchedulerDb instance = null;
    
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
    
    public String addMessage(String id, String message, int weight) {
        if (getMessage(id) != null) {
            return "ERROR: Message ID already exists.";
        }

        Document document = new Document(ID_KEY, id)
                .append(MESSAGE_KEY, message)
                .append(WEIGHT_KEY, weight);
        insertOne(document);
        return String.format("Successfully added \"%s\" to the list of scheduled messages.", id);
    }

    public String editMessage(String id, String message) {
        if (getMessage(id) == null) {
            return "ERROR: Message ID doesn't exist.";
        }

        Document document = new Document(ID_KEY, id)
                .append(MESSAGE_KEY, message);
        updateOne(id, document);
        return String.format("Successfully edited scheduled message for \"%s\".", id);
    }

    public String editMessage(String id, int weight) {
        if (getMessage(id) == null) {
            return "ERROR: Message ID doesn't exist.";
        }

        Document document = new Document(ID_KEY, id)
                .append(WEIGHT_KEY, weight);
        updateOne(id, document);
        return String.format("Successfully edited weight for scheduled message \"%s\".", id);
    }
    
    public String deleteMessage(String id) {
        if (getMessage(id) == null) {
            return "ERROR: Message ID doesn't exist.";
        }
        deleteOne(id);
        return String.format("Successfully deleted scheduled message \"%s\".", id);
    }
    
    public ScheduledMessage getMessage(String id) {
        Document result = findFirstEquals(ID_KEY, id);
        if (result != null) {
            String message = result.getString(MESSAGE_KEY);
            int weight = result.getInteger(WEIGHT_KEY) == null ? result.getInteger(WEIGHT_KEY) : 1;
            return new ScheduledMessage(id, message, weight);
        }
        return null;
    }
    
    public ArrayList<ScheduledMessage> getAllMessages() {
        MongoCursor<Document> result = findAll().iterator();
        ArrayList<ScheduledMessage> messages = new ArrayList<>();
        
        while (result.hasNext()) {
            Document doc = result.next();
            String id = doc.getString(ID_KEY);
            String message = doc.getString(MESSAGE_KEY);
            int weight = doc.getInteger(WEIGHT_KEY) == null ? doc.getInteger(WEIGHT_KEY) : 1;
            messages.add(new ScheduledMessage(id, message, weight));
        }
        return messages;
    }
}
