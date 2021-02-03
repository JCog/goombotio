package listeners.commands;

import com.gikk.twirk.types.twitchMessage.TwitchMessage;
import com.gikk.twirk.types.users.TwitchUser;
import com.jcog.utils.TwitchUserLevel;
import com.jcog.utils.database.DbManager;
import com.jcog.utils.database.misc.SocialSchedulerDb;
import util.TwirkInterface;

import java.util.concurrent.ScheduledExecutorService;

public class ScheduledMessageManagerListener extends CommandBase {
    private static final String PATTERN = "!scheduled";

    private final SocialSchedulerDb socialSchedulerDb;
    private final TwirkInterface twirk;

    private enum FUNCTION {
        ADD,
        EDIT,
        DELETE
    }

    public ScheduledMessageManagerListener(ScheduledExecutorService scheduler,
                                           TwirkInterface twirk,
                                           DbManager dbManager) {
        super(CommandType.PREFIX_COMMAND, scheduler);
        this.twirk = twirk;
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
    protected void performCommand(String command, TwitchUser sender, TwitchMessage message) {
        String[] messageSplit = message.getContent().split("\\s", 4);
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
                twirk.channelMessage(socialSchedulerDb.addMessage(idString, content, 1));
                break;
            case EDIT:
                if (!hasContent) {
                    showError("no content");
                    return;
                }
                twirk.channelMessage(socialSchedulerDb.editMessage(idString, content));
                break;
            case DELETE:
                twirk.channelMessage(socialSchedulerDb.deleteMessage(idString));
                break;
        }
    }

    private void showError(String error) {
        twirk.channelMessage(String.format("ERROR: %s", error));
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
