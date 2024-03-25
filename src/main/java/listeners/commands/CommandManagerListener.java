package listeners.commands;

import com.github.twitch4j.chat.events.channel.ChannelMessageEvent;
import database.DbManager;
import database.misc.CommandDb;
import database.misc.CommandDb.CommandItem;
import util.TwitchApi;
import util.TwitchUserLevel;

import java.util.HashSet;
import java.util.Set;

import static util.TwitchUserLevel.USER_LEVEL;

public class CommandManagerListener extends CommandBase {
    private static final CommandType COMMAND_TYPE = CommandType.PREFIX_COMMAND;
    private static final USER_LEVEL MIN_USER_LEVEL = USER_LEVEL.DEFAULT;
    private static final int MANAGER_COOLDOWN = 0;
    private static final CooldownType COOLDOWN_TYPE = CooldownType.COMBINED;
    private final static String PATTERN = "!commands";
    
    private final static String USER_LEVEL_TAG = "-ul=";
    private final static String COOLDOWN_TAG = "-cd=";
    private final static long DEFAULT_COOLDOWN = 2; // seconds

    private final CommandDb commandDb;
    private final TwitchApi twitchApi;

    private enum FUNCTION {
        ADD,
        EDIT,
        DELETE,
        DETAILS
    }

    public CommandManagerListener(TwitchApi twitchApi, DbManager dbManager) {
        super(COMMAND_TYPE, MIN_USER_LEVEL, MANAGER_COOLDOWN, COOLDOWN_TYPE, PATTERN);
        this.twitchApi = twitchApi;
        this.commandDb = dbManager.getCommandDb();
    }

    @Override
    protected void performCommand(String command, USER_LEVEL commandUserLevel, ChannelMessageEvent messageEvent) {
        String[] messageSplit = messageEvent.getMessage().split("\\s", 4);
        if (messageSplit.length == 1) {
            twitchApi.channelMessage("This is a JSON endpoint, so if it looks unreadable you'll probably need a browser extension for viewing JSON. https://www.jcoggers.com/commands");
            return;
        }
        if (commandUserLevel.value < USER_LEVEL.MOD.value) {
            return;
        }
        if (messageSplit.length < 3) {
            showError("missing arguments");
            return;
        }

        String typeString = messageSplit[1];
        String idString = messageSplit[2].toLowerCase();
        if (!isValidId(idString)) {
            showError("invalid command ID");
            return;
        }

        if (CommandBase.getReservedCommands().contains(idString)) {
            showError(String.format("\"%s\" is a reserved command id and cannot be modified.", idString));
            return;
        }

        String content = null;
        String[] parameterStrings = null;

        try {
            int start = messageSplit[3].indexOf('"');
            int end = messageSplit[3].lastIndexOf('"');
            if (start != end) { //valid quotes
                content = messageSplit[3].substring(start + 1, end);
                parameterStrings = messageSplit[3].replace(messageSplit[3].substring(start, end + 1), "").split("\\s");
            } else if (start == -1) { //no quotes
                parameterStrings = messageSplit[3].split("\\s");
            } else { //one quote mark
                showError("unbalanced quotation mark");
                return;
            }
        } catch (IndexOutOfBoundsException e) {
            //do nothing
        }
        boolean hasContent = content != null && !content.isEmpty();

        FUNCTION type = getFunction(typeString);
        if (type == null) {
            showError("invalid function");
            return;
        }

        boolean hasUserLevel = hasUserLevelTag(parameterStrings);
        boolean hasCooldown = hasCooldownTag(parameterStrings);
        USER_LEVEL userLevel = getUserLevel(parameterStrings);
        Long cooldown = getCooldown(parameterStrings);
        if (hasUserLevel || hasCooldown) {
            String invalidTag = getInvalidTag(parameterStrings);
            if (invalidTag != null) {
                showError(String.format("invalid parameter \"%s\"", invalidTag));
                return;
            }
    
            String duplicateTag = getDuplicateTag(parameterStrings);
            if (duplicateTag != null) {
                showError(String.format("duplicate tag \"%s\"", duplicateTag));
                return;
            }
        }

        if (userLevel == null) {
            showError("invalid user level");
            return;
        }
        if (cooldown == null) {
            showError("invalid cooldown");
            return;
        }

        switch (type) {
            case ADD:
                if (!hasContent) {
                    showError("no content");
                    return;
                }
                twitchApi.channelMessage(commandDb.addCommand(idString, content, cooldown, userLevel));
                break;
            case EDIT:
                if (hasContent && hasCooldown && hasUserLevel) {
                    twitchApi.channelMessage(commandDb.editCommand(idString, content, cooldown, userLevel));
                } else if (hasContent && hasCooldown) {
                    twitchApi.channelMessage(commandDb.editCommand(idString, content, cooldown));
                } else if (hasContent && hasUserLevel) {
                    twitchApi.channelMessage(commandDb.editCommand(idString, content, userLevel));
                } else if (hasCooldown && hasUserLevel) {
                    twitchApi.channelMessage(commandDb.editCommand(idString, cooldown, userLevel));
                } else if (hasContent) {
                    twitchApi.channelMessage(commandDb.editCommand(idString, content));
                } else if (hasCooldown) {
                    twitchApi.channelMessage(commandDb.editCommand(idString, cooldown));
                } else if (hasUserLevel) {
                    twitchApi.channelMessage(commandDb.editCommand(idString, userLevel));
                } else {
                    showError("nothing to edit");
                }
                break;
            case DELETE:
                twitchApi.channelMessage(commandDb.deleteCommand(idString));
                break;
            case DETAILS:
                CommandItem commandItem = commandDb.getCommandItem(idString);
                if (commandItem == null) {
                    showError(String.format("unknown command \"%s\"", idString));
                    return;
                }
                twitchApi.channelMessage(String.format(
                        "\"%s\" -ul=%s -cd=%d",
                        commandItem.getMessage(),
                        commandItem.getPermission(),
                        commandItem.getCooldown()
                ));
                break;
        }
    }

    private void showError(String error) {
        twitchApi.channelMessage(String.format("ERROR: %s", error));
    }

    private FUNCTION getFunction(String function) {
        switch (function) {
            case "add":
                return FUNCTION.ADD;
            case "edit":
                return FUNCTION.EDIT;
            case "delete":
                return FUNCTION.DELETE;
            case "details":
                return FUNCTION.DETAILS;
            default:
                return null;
        }
    }

    private boolean isValidId(String id) {
        return id.matches("![a-zA-Z0-9]+");
    }

    private boolean hasUserLevelTag(String[] parameters) {
        if (parameters == null) {
            return false;
        }
        for (String param : parameters) {
            if (param.startsWith(USER_LEVEL_TAG)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasCooldownTag(String[] parameters) {
        if (parameters == null) {
            return false;
        }
        for (String param : parameters) {
            if (param.startsWith(COOLDOWN_TAG)) {
                return true;
            }
        }
        return false;
    }

    //search everything after the command id for a permission - returns null if an invalid type is found
    private USER_LEVEL getUserLevel(String[] parameters) {
        if (parameters == null) {
            return USER_LEVEL.DEFAULT;
        }
        for (String param : parameters) {
            if (param.startsWith(USER_LEVEL_TAG)) {
                int start = USER_LEVEL_TAG.length();
                String type = param.substring(start);
                return TwitchUserLevel.getUserLevel(type);
            }
        }
        return USER_LEVEL.DEFAULT;
    }

    //returns the first invalid tag, null if there are none
    private String getInvalidTag(String[] parameters) {
        if (parameters == null) {
            return null;
        }
        for (String param : parameters) {
            if (!(param.startsWith(USER_LEVEL_TAG) || param.startsWith(COOLDOWN_TAG))) {
                return param;
            }
        }
        return null;
    }

    //returns duplicate tag, null if there are none. assumes no invalid tags
    private String getDuplicateTag(String[] parameters) {
        if (parameters == null) {
            return null;
        }
        Set<String> tags = new HashSet<>();
        for (String param : parameters) {
            String paramTag = param.substring(0, param.indexOf('='));
            if (tags.contains(paramTag)) {
                return paramTag;
            } else {
                tags.add(paramTag);
            }
        }
        return null;
    }

    private Long getCooldown(String[] parameters) {
        if (parameters == null) {
            return DEFAULT_COOLDOWN;
        }
        for (String param : parameters) {
            if (param.startsWith(COOLDOWN_TAG)) {
                int start = COOLDOWN_TAG.length();
                String cooldownString = param.substring(start);
                try {
                    return Long.parseLong(cooldownString);
                } catch (NumberFormatException e) {
                    return null;
                }
            }
        }
        return DEFAULT_COOLDOWN;
    }
}
