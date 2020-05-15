package Database;

import Database.Entries.CommandItem;
import Util.TwitchUserLevel;
import com.mongodb.client.MongoCollection;
import org.bson.Document;

public class CommandDb extends CollectionBase {
    
    private static final String COLLECTION_NAME = "commands";
    private static final String MESSAGE_KEY = "message";
    private static final String PERMISSION_KEY = "permission";
    private static final String COUNT_KEY = "count";
    private static final String COOLDOWN_KEY = "cooldown";
    
    private final static long DEFAULT_COOLDOWN = 2 * 1000;
    
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
    
    public String addMessage(String id, String message, TwitchUserLevel.USER_LEVEL permission) {
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
    
    public String editCommand(String id, String message, TwitchUserLevel.USER_LEVEL permission) {
        if (getCommand(id) == null) {
            return "ERROR: Message ID doesn't exist.";
        }
    
        Document document = new Document(ID_KEY, id)
                .append(MESSAGE_KEY, message)
                .append(PERMISSION_KEY, permission.value);
        updateOne(id, document);
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
        updateOne(id, document);
        return String.format("Successfully edited command message for \"%s\".", id);
    }
    
    public String editPermission(String id, TwitchUserLevel.USER_LEVEL permission) {
        if (getCommand(id) == null) {
            return "ERROR: Message ID doesn't exist.";
        }
        
        Document document = new Document(ID_KEY, id)
                .append(PERMISSION_KEY, permission.value);
        updateOne(id, document);
        return String.format("Successfully edited command permission for \"%s\".", id);
    }
    
    public String editPermission(String id, String permission) {
        if (getCommand(id) == null) {
            return "ERROR: Message ID doesn't exist.";
        }
        
        Document document = new Document(ID_KEY, id)
                .append(PERMISSION_KEY, getPermission(permission).value);
        updateOne(id, document);
        return String.format("Successfully edited command permission for \"%s\" to %s.", id, permission);
    }
    
    public String editTimeout(String id, long ms) {
        if (getCommand(id) == null) {
            return "ERROR: Message ID doesn't exist.";
        }
    
        Document document = new Document(ID_KEY, id)
                .append(COOLDOWN_KEY, ms);
        updateOne(id, document);
        return String.format("Successfully edited cooldown length for \"%s\" to %d ms.", id, ms);
    }
    
    public String deleteMessage(String id) {
        if (getCommand(id) == null) {
            return "ERROR: Message ID doesn't exist.";
        }
        deleteOne(id);
        return String.format("Successfully deleted command \"%s\".", id);
    }
    
    public void incrementCount(String id) {
        CommandItem commandItem = getCommandItem(id);
        if (commandItem != null) {
            updateOne(id, new Document(COUNT_KEY, commandItem.getCount() + 1));
        }
    }
    
    public CommandItem getCommandItem(String id) {
        Document result = getCommand(id);
        if (result != null) {
            return new CommandItem(
                    result.getString(ID_KEY),
                    result.getString(MESSAGE_KEY),
                    CommandItem.getUserLevel(result.getInteger(PERMISSION_KEY)),
                    result.containsKey(COOLDOWN_KEY) ? result.getLong(COOLDOWN_KEY) : DEFAULT_COOLDOWN,
                    result.containsKey(COUNT_KEY) ? result.getInteger(COUNT_KEY) : 0
            );
        }
        return null;
    }
    
    private Document getCommand(String id) {
        return findFirstEquals(ID_KEY, id);
    }
    
    private TwitchUserLevel.USER_LEVEL getPermission(String permission) {
        switch (permission) {
            case "sub":
                return TwitchUserLevel.USER_LEVEL.SUBSCRIBER;
            case "vip":
                return TwitchUserLevel.USER_LEVEL.VIP;
            case "mod":
                return TwitchUserLevel.USER_LEVEL.MOD;
            case "owner":
                return TwitchUserLevel.USER_LEVEL.BROADCASTER;
            default:
                return TwitchUserLevel.USER_LEVEL.DEFAULT;
        }
    }
    
    private String getPermission(TwitchUserLevel.USER_LEVEL permission) {
        switch (permission) {
            case SUBSCRIBER:
                return "sub";
            case VIP:
                return "vip";
            case MOD:
                return "mod";
            case BROADCASTER:
                return "owner";
            default:
                return "default";
        }
    }
}
