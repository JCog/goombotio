package Util.Database;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import org.bson.Document;

import java.util.ArrayList;

import static com.mongodb.client.model.Filters.eq;

public class CommandDb extends CollectionBase {
    
    private static final String COLLECTION_NAME = "commands";
    private static final String ID_KEY = "_id";
    private static final String MESSAGE_KEY = "message";
    
    private static CommandDb instance = null;
    
    private CommandDb() {
        super();
    }
    
    public static CommandDb getInstance() {
        if (instance == null) {
            instance = new CommandDb();
        }
        return instance;
    }
    
    @Override
    protected MongoCollection<Document> setCollection() {
        return goombotioDb.getCollection(COLLECTION_NAME);
    }
    
    public String addMessage(String id, String message) {
        if (getMessage(id) != null) {
            return "ERROR: Message ID already exists.";
        }
        
        Document document = new Document(ID_KEY, id)
                .append(MESSAGE_KEY, message);
        insertOne(document);
        return String.format("Successfully added \"%s\" to the list of commands.", id);
    }
    
    public String editMessage(String id, String message) {
        if (getMessage(id) == null) {
            return "ERROR: Message ID doesn't exist.";
        }
    
        Document document = new Document(ID_KEY, id)
                .append(MESSAGE_KEY, message);
        updateOne(eq(ID_KEY, id), new Document("$set", document));
        return String.format("Successfully edited command \"%s\".", id);
    }
    
    public String deleteMessage(String id) {
        if (getMessage(id) == null) {
            return "ERROR: Message ID doesn't exist.";
        }
        deleteOne(eq(ID_KEY, id));
        return String.format("Successfully deleted command \"%s\".", id);
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
