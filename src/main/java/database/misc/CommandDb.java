package database.misc;

import com.mongodb.client.model.Sorts;
import database.GbCollection;
import database.GbDatabase;
import org.bson.Document;
import org.jetbrains.annotations.Nullable;
import util.TwitchUserLevel;

import java.util.ArrayList;
import java.util.List;

public class CommandDb extends GbCollection {
    private static final String COLLECTION_NAME = "commands";
    
    private static final String MESSAGE_KEY = "message";
    private static final String PERMISSION_KEY = "permission";
    private static final String COUNT_KEY = "count";
    private static final String COOLDOWN_KEY = "cooldown";

    private final static long DEFAULT_COOLDOWN = 2; // seconds

    public CommandDb(GbDatabase gbDatabase) {
        super(gbDatabase, COLLECTION_NAME);
    }

    public String addCommand(String id, String message, Long cooldown, TwitchUserLevel.USER_LEVEL userLevel) {
        if (getCommand(id) != null) {
            return "ERROR: Message ID already exists.";
        }

        Document document = new Document(ID_KEY, id)
                .append(MESSAGE_KEY, message)
                .append(COOLDOWN_KEY, cooldown)
                .append(PERMISSION_KEY, userLevel.value);
        insertOne(document);
        return String.format(
                "Successfully added \"%s\" to the list of commands with cooldown %ds and user level \"%s\".",
                id,
                cooldown,
                userLevel
        );
    }

    public String editCommand(String id, String message, Long cooldown, TwitchUserLevel.USER_LEVEL userLevel) {
        if (getCommand(id) == null) {
            return "ERROR: Message ID doesn't exist.";
        }

        Document document = new Document(ID_KEY, id);
        if (message != null) {
            document.append(MESSAGE_KEY, message);
        }
        if (cooldown != null) {
            document.append(COOLDOWN_KEY, cooldown);
        }
        if (userLevel != null) {
            document.append(PERMISSION_KEY, userLevel.value);
        }
        updateOne(id, document);
        return String.format("Successfully edited command \"%s\"", id);

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

    @Nullable
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

    public List<String> getAllCommandIds() {
        List<String> commands = new ArrayList<>();
        for (Document document : findAll().sort(Sorts.ascending(ID_KEY))) {
            commands.add(document.getString(ID_KEY));
        }
        return commands;
    }

    private Document getCommand(String id) {
        return findFirstEquals(ID_KEY, id);
    }
    
    public static class CommandItem {
        private final String id;
        private final String message;
        private final TwitchUserLevel.USER_LEVEL permission;
        private final long cooldown;
        private final int count;
        
        @Nullable
        public static TwitchUserLevel.USER_LEVEL getUserLevel(int permission) {
            switch (permission) {
                case 0:
                    return TwitchUserLevel.USER_LEVEL.DEFAULT;
                case 2:
                    return TwitchUserLevel.USER_LEVEL.SUBSCRIBER;
                case 4:
                    return TwitchUserLevel.USER_LEVEL.STAFF;
                case 5:
                    return TwitchUserLevel.USER_LEVEL.VIP;
                case 6:
                    return TwitchUserLevel.USER_LEVEL.MOD;
                case 9:
                    return TwitchUserLevel.USER_LEVEL.BROADCASTER;
                default:
                    return null;
            }
        }
        
        public CommandItem(String id, String message, TwitchUserLevel.USER_LEVEL permission, long cooldown, int count) {
            this.id = id;
            this.message = message;
            this.permission = permission;
            this.cooldown = cooldown;
            this.count = count;
        }
        
        public String getId() {
            return id;
        }
        
        public String getMessage() {
            return message;
        }
        
        public TwitchUserLevel.USER_LEVEL getPermission() {
            return permission;
        }
        
        public long getCooldown() {
            return cooldown;
        }
        
        public int getCount() {
            return count;
        }
    }
    
}
