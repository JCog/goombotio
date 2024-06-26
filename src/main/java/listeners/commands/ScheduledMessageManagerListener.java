package listeners.commands;

import com.github.twitch4j.chat.events.channel.ChannelMessageEvent;
import database.misc.SocialSchedulerDb;
import util.CommonUtils;
import util.TwitchApi;
import util.TwitchUserLevel.USER_LEVEL;

public class ScheduledMessageManagerListener extends CommandBase {
    private static final CommandType COMMAND_TYPE = CommandType.PREFIX_COMMAND;
    private static final USER_LEVEL MIN_USER_LEVEL = USER_LEVEL.MOD;
    private static final int COOLDOWN = 0;
    private static final CooldownType COOLDOWN_TYPE = CooldownType.GLOBAL;
    private static final String PATTERN = "!scheduled";

    private final SocialSchedulerDb socialSchedulerDb;
    private final TwitchApi twitchApi;

    private enum FUNCTION {
        ADD,
        EDIT,
        DELETE
    }

    public ScheduledMessageManagerListener(CommonUtils commonUtils) {
        super(COMMAND_TYPE, MIN_USER_LEVEL, COOLDOWN, COOLDOWN_TYPE, PATTERN);
        twitchApi = commonUtils.twitchApi();
        socialSchedulerDb = commonUtils.dbManager().getSocialSchedulerDb();
    }

    @Override
    protected void performCommand(String command, USER_LEVEL userLevel, ChannelMessageEvent messageEvent) {
        String[] messageSplit = messageEvent.getMessage().split("\\s", 4);
        if (messageSplit.length < 3) {
            showError("missing arguments");
            return;
        }

        String typeString = messageSplit[1];
        String idString = messageSplit[2];
        if (!isValidId(idString)) {
            showError("invalid message ID");
            return;
        }

        String content = null;
        try {
            int start = messageSplit[3].indexOf('"');
            int end = messageSplit[3].lastIndexOf('"');
            if (start != end) { //valid quotes
                content = messageSplit[3].substring(start + 1, end);
            } else if (start == -1) { //no quotes
                showError("no quotation marks");
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
    
        switch (type) {
            case ADD -> {
                if (!hasContent) {
                    showError("no content");
                    return;
                }
                twitchApi.channelMessage(socialSchedulerDb.addMessage(idString, content, 1));
            }
            case EDIT -> {
                if (!hasContent) {
                    showError("no content");
                    return;
                }
                twitchApi.channelMessage(socialSchedulerDb.editMessage(idString, content));
            }
            case DELETE -> twitchApi.channelMessage(socialSchedulerDb.deleteMessage(idString));
        }
    }

    private void showError(String error) {
        twitchApi.channelMessage(String.format("ERROR: %s", error));
    }

    private FUNCTION getFunction(String function) {
        return switch (function) {
            case "add" -> FUNCTION.ADD;
            case "edit" -> FUNCTION.EDIT;
            case "delete" -> FUNCTION.DELETE;
            default -> null;
        };
    }

    private boolean isValidId(String id) {
        return id.matches("[a-zA-Z0-9]+");
    }
}
