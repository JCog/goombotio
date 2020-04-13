package Util.Database;

import Util.Database.Entries.CommandItem;
import com.gikk.twirk.enums.USER_TYPE;
import com.mongodb.client.MongoCollection;
import org.bson.Document;

import static com.mongodb.client.model.Filters.eq;

public class CommandDb extends CollectionBase {
    
    private static final String COLLECTION_NAME = "commands";
    private static final String ID_KEY = "_id";
    private static final String MESSAGE_KEY = "message";
    private static final String PERMISSION_KEY = "permission";
    
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
    
    public String addMessage(String id, String message, USER_TYPE permission) {
        if (getCommand(id) != null) {
            return "ERROR: Message ID already exists.";
        }
        
        Document document = new Document(ID_KEY, id)
                .append(MESSAGE_KEY, message)
                .append(PERMISSION_KEY, permission.value);
        insertOne(document);
        return String.format(
                "Successfully added \"%s\" to the list of commands with permission \"%s\".",
                id,
                getPermission(permission)
        );
    }
    
    public String addMessage(String id, String message, String permission) {
        return addMessage(id, message, getPermission(permission));
    }
    
    public String editCommand(String id, String message, USER_TYPE permission) {
        if (getCommand(id) == null) {
            return "ERROR: Message ID doesn't exist.";
        }
    
        Document document = new Document(ID_KEY, id)
                .append(MESSAGE_KEY, message)
                .append(PERMISSION_KEY, permission.value);
        updateOne(eq(ID_KEY, id), new Document("$set", document));
        return String.format(
                "Successfully edited command message for \"%s\" and set permission to \"%s\".",
                id,
                getPermission(permission)
        );
        
    }
    
    public String editCommand(String id, String message, String permission) {
        return editCommand(id, message, getPermission(permission));
    }
    
    public String editMessage(String id, String message) {
        if (getCommand(id) == null) {
            return "ERROR: Message ID doesn't exist.";
        }
    
        Document document = new Document(ID_KEY, id)
                .append(MESSAGE_KEY, message);
        updateOne(eq(ID_KEY, id), new Document("$set", document));
        return String.format("Successfully edited command message for \"%s\".", id);
    }
    
    public String editPermission(String id, USER_TYPE permission) {
        if (getCommand(id) == null) {
            return "ERROR: Message ID doesn't exist.";
        }
        
        Document document = new Document(ID_KEY, id)
                .append(PERMISSION_KEY, permission.value);
        updateOne(eq(ID_KEY, id), new Document("$set", document));
        return String.format("Successfully edited command permission for \"%s\".", id);
    }
    
    public String editPermission(String id, String permission) {
        if (getCommand(id) == null) {
            return "ERROR: Message ID doesn't exist.";
        }
        
        Document document = new Document(ID_KEY, id)
                .append(PERMISSION_KEY, getPermission(permission).value);
        updateOne(eq(ID_KEY, id), new Document("$set", document));
        return String.format("Successfully edited command permission for \"%s\" to %s.", id, permission);
    }
    
    public String deleteMessage(String id) {
        if (getCommand(id) == null) {
            return "ERROR: Message ID doesn't exist.";
        }
        deleteOne(eq(ID_KEY, id));
        return String.format("Successfully deleted command \"%s\".", id);
    }
    
    public CommandItem getCommandItem(String id) {
        Document result = getCommand(id);
        if (result != null) {
            return new CommandItem(
                    result.getString(ID_KEY),
                    result.getString(MESSAGE_KEY),
                    CommandItem.getUserType(result.getInteger(PERMISSION_KEY))
            );
        }
        return null;
    }
    
    private Document getCommand(String id) {
        return find(eq(ID_KEY, id)).first();
    }
    
    private USER_TYPE getPermission(String permission) {
        switch (permission) {
            case "sub":
                return USER_TYPE.SUBSCRIBER;
            case "mod":
                return USER_TYPE.MOD;
            case "owner":
                return USER_TYPE.OWNER;
            default:
                return USER_TYPE.DEFAULT;
        }
    }
    
    private String getPermission(USER_TYPE permission) {
        switch (permission) {
            case SUBSCRIBER:
                return "sub";
            case MOD:
                return "mod";
            case OWNER:
                return "owner";
            default:
                return "default";
        }
    }
}
