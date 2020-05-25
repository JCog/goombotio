package Database.Misc;

import Database.CollectionBase;
import Database.Entries.CommandItem;
import Util.TwitchUserLevel;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Sorts;
import org.bson.Document;

import java.util.ArrayList;

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
    
    public String addMessage(String id, String message, long cooldown, TwitchUserLevel.USER_LEVEL userLevel) {
        if (getCommand(id) != null) {
            return "ERROR: Message ID already exists.";
        }
        
        Document document = new Document(ID_KEY, id)
                .append(MESSAGE_KEY, message)
                .append(COOLDOWN_KEY, cooldown)
                .append(PERMISSION_KEY, userLevel.value);
        insertOne(document);
        return String.format(
                "Successfully added \"%s\" to the list of commands with cooldown %d ms and user level \"%s\".",
                id,
                cooldown,
                userLevel.toString()
        );
    }
    
    public String editCommand(String id, String message, long cooldown, TwitchUserLevel.USER_LEVEL userLevel) {
        if (getCommand(id) == null) {
            return "ERROR: Message ID doesn't exist.";
        }
        
        Document document = new Document(ID_KEY, id)
                .append(MESSAGE_KEY, message)
                .append(COOLDOWN_KEY, cooldown)
                .append(PERMISSION_KEY, userLevel.value);
        updateOne(id, document);
        return String.format(
                "Successfully edited command message for \"%s\", set cooldown to %d ms, and set user level to \"%s\".",
                id,
                cooldown,
                userLevel.toString()
        );
        
    }
    
    public String editCommand(String id, String message, long cooldown) {
        if (getCommand(id) == null) {
            return "ERROR: Message ID doesn't exist.";
        }
        
        Document document = new Document(ID_KEY, id)
                .append(MESSAGE_KEY, message)
                .append(COOLDOWN_KEY, cooldown);
        updateOne(id, document);
        return String.format(
                "Successfully edited command message for \"%s\" and set cooldown to %d ms.",
                id,
                cooldown
        );
        
    }
    
    public String editCommand(String id, String message, TwitchUserLevel.USER_LEVEL userLevel) {
        if (getCommand(id) == null) {
            return "ERROR: Message ID doesn't exist.";
        }
    
        Document document = new Document(ID_KEY, id)
                .append(MESSAGE_KEY, message)
                .append(PERMISSION_KEY, userLevel.value);
        updateOne(id, document);
        return String.format(
                "Successfully edited command message for \"%s\" and set user level to \"%s\".",
                id,
                userLevel.toString()
        );
        
    }
    
    public String editCommand(String id, long cooldown, TwitchUserLevel.USER_LEVEL userLevel) {
        if (getCommand(id) == null) {
            return "ERROR: Message ID doesn't exist.";
        }
        
        Document document = new Document(ID_KEY, id)
                .append(COOLDOWN_KEY, cooldown)
                .append(PERMISSION_KEY, userLevel.value);
        updateOne(id, document);
        return String.format(
                "Successfully edited \"%s\": set cooldown to %d ms and set user level to \"%s\".",
                id,
                cooldown,
                userLevel.toString()
        );
        
    }
    
    public String editCommand(String id, String message) {
        if (getCommand(id) == null) {
            return "ERROR: Message ID doesn't exist.";
        }
    
        Document document = new Document(ID_KEY, id)
                .append(MESSAGE_KEY, message);
        updateOne(id, document);
        return String.format("Successfully edited command message for \"%s\".", id);
    }
    
    public String editCommand(String id, long cooldown) {
        if (getCommand(id) == null) {
            return "ERROR: Message ID doesn't exist.";
        }
        
        Document document = new Document(ID_KEY, id)
                .append(COOLDOWN_KEY, cooldown);
        updateOne(id, document);
        return String.format("Successfully edited \"%s\": set cooldown to %d ms.", id, cooldown);
    }
    
    public String editCommand(String id, TwitchUserLevel.USER_LEVEL userLevel) {
        if (getCommand(id) == null) {
            return "ERROR: Message ID doesn't exist.";
        }
        
        Document document = new Document(ID_KEY, id)
                .append(PERMISSION_KEY, userLevel.value);
        updateOne(id, document);
        return String.format("Successfully edited \"%s\": set user level to \"%s\".", id, userLevel.toString());
    }
    
    public String deleteCommand(String id) {
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
    
    public ArrayList<String> getAllCommandIds() {
        ArrayList<String> commands = new ArrayList<>();
        for(Document document : findAll().sort(Sorts.ascending(ID_KEY))) {
            commands.add(document.getString(ID_KEY));
        }
        return commands;
    }
    
    private Document getCommand(String id) {
        return findFirstEquals(ID_KEY, id);
    }
}
