package database.misc;

import database.GbCollection;
import database.GbDatabase;
import org.bson.Document;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static util.TwitchUserLevel.USER_LEVEL;
import static util.TwitchUserLevel.getUserLevel;

public class CommandDb extends GbCollection {
    private static final String COLLECTION_NAME = "commands";
    
    private static final String ALIASES_KEY = "aliases";
    private static final String MESSAGE_KEY = "message";
    private static final String PERMISSION_KEY = "permission";
    private static final String COUNT_KEY = "count";
    private static final String COOLDOWN_KEY = "cooldown";

    private final static long DEFAULT_COOLDOWN = 2; // seconds

    public CommandDb(GbDatabase gbDatabase) {
        super(gbDatabase, COLLECTION_NAME);
    }
    
    public String addAlias(String commandId, String aliasId) {
        Document command = getCommand(commandId);
        if (command == null) {
            return "ERROR: Command ID does not exist.";
        }
        if (getCommand(aliasId) != null) {
            return "ERROR: Alias ID already exists.";
        }
        
        pushItemToArray(command.getObjectId(ID_KEY), ALIASES_KEY, aliasId);
        return String.format("Successfully added \"%s\" as an alias to \"%s\"", aliasId, commandId);
    }
    
    public String deleteAlias(String aliasId) {
        Document command = getCommand(aliasId);
        if (command == null) {
            return "ERROR: Alias ID does not exist.";
        }
        List<String> aliasList = command.getList(ALIASES_KEY, String.class);
        if (aliasList.size() == 1) {
            return "ERROR: Last known alias of this command. Use !delcom instead.";
        }
        pullItemFromArray(command.getObjectId(ID_KEY), ALIASES_KEY, aliasId);
        return String.format("Successfully removed alias \"%s\"", aliasId);
    }

    public String addCommand(
            String id,
            String message,
            Long cooldown,
            USER_LEVEL userLevel,
            Integer count,
            String... aliases
    ) {
        
        if (getCommand(id) != null) {
            return "ERROR: Message ID already exists.";
        }
        
        List<String> aliasesList = new ArrayList<>(Arrays.asList(aliases));
        aliasesList.add(id);
        insertOne(new Document(ALIASES_KEY, aliasesList)
                .append(MESSAGE_KEY, message)
                .append(COOLDOWN_KEY, cooldown)
                .append(PERMISSION_KEY, userLevel.value)
                .append(COUNT_KEY, count)
        );
        
        StringBuilder aliasesString = new StringBuilder();
        if (aliases.length == 1) {
            aliasesString.append(String.format(" alias \"%s\",", aliases[0]));
        } else if (aliases.length == 2) {
            aliasesString.append(String.format(" aliases \"%s\" and \"%s\",", aliases[0], aliases[1]));
        } else if (aliases.length > 2) {
            aliasesString.append(" aliases");
            for (int i = 0; i < aliases.length - 1; i++) {
                aliasesString.append(String.format(" \"%s\",", aliases[i]));
            }
            aliasesString.append(String.format(" and \"%s\",", aliases[aliases.length - 1]));
        }
        
        return String.format(
                "Successfully added \"%s\" to the list of commands with%s cooldown %ds and user level \"%s\".",
                id,
                aliasesString,
                cooldown,
                userLevel
        );
    }

    public String editCommand(String id, String message, Long cooldown, USER_LEVEL userLevel) {
        Document command = getCommand(id);
        if (command == null) {
            return "ERROR: Message ID doesn't exist.";
        }

        if (message != null) {
            updateField(command.getObjectId(ID_KEY), MESSAGE_KEY, message);
        }
        if (cooldown != null) {
            updateField(command.getObjectId(ID_KEY), COOLDOWN_KEY, cooldown);
        }
        if (userLevel != null) {
            updateField(command.getObjectId(ID_KEY), PERMISSION_KEY, userLevel.value);
        }
        return String.format("Successfully edited command \"%s\".", id);

    }

    public String deleteCommand(String id) {
        Document command = getCommand(id);
        if (command == null) {
            return "ERROR: Message ID doesn't exist.";
        }
        deleteOne(command.getObjectId(ID_KEY));
        return String.format("Successfully deleted command \"%s\".", id);
    }

    public void incrementCount(String alias) {
        Document command = getCommand(alias);
        if (command != null) {
            int curCount = command.getInteger(COUNT_KEY) == null ? 0 : command.getInteger(COUNT_KEY);
            updateField(command.getObjectId(ID_KEY), COUNT_KEY, curCount + 1);
        }
    }

    @Nullable
    public CommandItem getCommandItem(String alias) {
        Document result = getCommand(alias);
        if (result == null) {
            return null;
        }
        return new CommandItem(
                result.getList(ALIASES_KEY, String.class),
                result.getString(MESSAGE_KEY),
                getUserLevel(result.getInteger(PERMISSION_KEY)),
                result.containsKey(COOLDOWN_KEY) ? result.getLong(COOLDOWN_KEY) : DEFAULT_COOLDOWN,
                result.containsKey(COUNT_KEY) ? result.getInteger(COUNT_KEY) : 0
        );
    }
    
    private Document getCommand(String alias) {
        return findFirstEquals(ALIASES_KEY, alias);
    }
    
    public record CommandItem(List<String> aliases, String message, USER_LEVEL permission, long cooldown, int count) {
        @Override
        public String toString() {
            return String.format(
                    "a=%s l=%s c=%d m=\"%s\"",
                    String.join(",", aliases),
                    permission,
                    cooldown,
                    message
            );
        }
    }
    
}
