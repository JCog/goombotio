package listeners.commands;

import com.github.twitch4j.chat.events.channel.ChannelMessageEvent;
import database.DbManager;
import database.misc.SocialSchedulerDb;
import util.TwitchApi;
import util.TwitchUserLevel;

import java.util.concurrent.ScheduledExecutorService;

public class ScheduledMessageManagerListener extends CommandBase {
    private static final String PATTERN = "!scheduled";

    private final SocialSchedulerDb socialSchedulerDb;
    private final TwitchApi twitchApi;

    private enum FUNCTION {
        ADD,
        EDIT,
        DELETE
    }

    public ScheduledMessageManagerListener(ScheduledExecutorService scheduler,
                                           TwitchApi twitchApi,
                                           DbManager dbManager) {
        super(CommandType.PREFIX_COMMAND, scheduler);
        this.twitchApi = twitchApi;
        socialSchedulerDb = dbManager.getSocialSchedulerDb();
    }

    @Override
    public String getCommandWords() {
        return PATTERN;
    }

    @Override
    protected TwitchUserLevel.USER_LEVEL getMinUserPrivilege() {
        return TwitchUserLevel.USER_LEVEL.MOD;
    }

    @Override
    protected int getCooldownLength() {
        return 0;
    }

    @Override
    protected void performCommand(String command, TwitchUserLevel.USER_LEVEL userLevel, ChannelMessageEvent messageEvent) {
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
            }
            else if (start == -1) { //no quotes
                showError("no quotation marks");
            }
            else { //one quote mark
                showError("unbalanced quotation mark");
                return;
            }
        }
        catch (IndexOutOfBoundsException e) {
            //do nothing
        }
        boolean hasContent = content != null && !content.isEmpty();

        FUNCTION type = getFunction(typeString);
        if (type == null) {
            showError("invalid function");
            return;
        }

        switch (type) {
            case ADD:
                if (!hasContent) {
                    showError("no content");
                    return;
                }
                twitchApi.channelMessage(socialSchedulerDb.addMessage(idString, content, 1));
                break;
            case EDIT:
                if (!hasContent) {
                    showError("no content");
                    return;
                }
                twitchApi.channelMessage(socialSchedulerDb.editMessage(idString, content));
                break;
            case DELETE:
                twitchApi.channelMessage(socialSchedulerDb.deleteMessage(idString));
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
            default:
                return null;
        }
    }

    private boolean isValidId(String id) {
        return id.matches("[a-zA-Z0-9]+");
    }
}
