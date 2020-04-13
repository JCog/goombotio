package Listeners.Commands;

import Util.Database.CommandDb;
import Util.Database.SocialSchedulerDb;
import com.gikk.twirk.Twirk;
import com.gikk.twirk.enums.USER_TYPE;
import com.gikk.twirk.types.twitchMessage.TwitchMessage;
import com.gikk.twirk.types.users.TwitchUser;

public class GoombotioCommandsListener extends CommandBase {
    
    private enum TYPE {
        SCHEDULED,
        COMMAND
    }
    private enum FUNCTION {
        ADD,
        EDIT,
        DELETE
    }
    
    private final static String[] validPermissions = {
            "default",
            "sub",
            "mod",
            "owner"
    };

    private final static String PATTERN = "!goombotio";
    
    private final Twirk twirk;
    private final SocialSchedulerDb ssdb;
    private final CommandDb commandDb;

    public GoombotioCommandsListener(Twirk twirk) {
        super(CommandType.PREFIX_COMMAND);
        this.twirk = twirk;
        this.ssdb = SocialSchedulerDb.getInstance();
        this.commandDb = CommandDb.getInstance();
    }

    @Override
    protected String getCommandWords() {
        return PATTERN;
    }

    @Override
    protected USER_TYPE getMinUserPrivilege() {
        return USER_TYPE.MOD;
    }

    @Override
    protected int getCooldownLength() {
        return 0;
    }

    @Override
    protected void performCommand(String command, TwitchUser sender, TwitchMessage message) {
        String[] messageSplit = message.getContent().split("\\s");
        if (messageSplit.length < 4) {
            showError("missing arguments");
            return;
        }
    
        FUNCTION function = getFunction(messageSplit[1]);
        if (function == null) {
            showError(String.format("unknown function \"%s\"", messageSplit[1]));
            return;
        }
        
        TYPE type = getType(messageSplit[2]);
        if (type == null) {
            showError(String.format("unknown type \"%s\"", messageSplit[2]));
            return;
        }
        
        String id = messageSplit[3];
        int start = message.getContent().indexOf('"') + 1;
        int end = message.getContent().lastIndexOf('"');
        String content = "";
        try {
            content = message.getContent().substring(start, end);
        }
        catch (StringIndexOutOfBoundsException e) {
            if (function != FUNCTION.DELETE && function != FUNCTION.EDIT) {
                showError("no content");
                return;
            }
        }
        boolean hasContent = !content.isEmpty();
        String permission = "";
        if (end + 1 < message.getContent().length()) {
            permission = messageSplit[messageSplit.length - 1];
        }
        boolean hasPermission = !permission.isEmpty();
        
        switch (type) {
            case SCHEDULED:
                switch (function) {
                    case ADD:
                        twirk.channelMessage(ssdb.addMessage(id, content));
                        break;
                    case EDIT:
                        twirk.channelMessage(ssdb.editMessage(id, content));
                        break;
                    case DELETE:
                        twirk.channelMessage(ssdb.deleteMessage(id));
                        break;
                }
            case COMMAND:
                switch (function) {
                    case ADD:
                        if (!hasContent) {
                            showError("no content");
                            return;
                        }
                        if (hasPermission) {
                            if (permissionIsValid(permission)) {
                                twirk.channelMessage(commandDb.addMessage(id, content, permission));
                            }
                            else {
                                showError("permission is invalid");
                            }
                        }
                        else {
                            twirk.channelMessage(commandDb.addMessage(id, content, USER_TYPE.DEFAULT));
                        }
                        break;
                    case EDIT:
                        if (hasPermission && !permissionIsValid(permission)) {
                            showError("permission is invalid");
                            return;
                        }
                        if (hasContent && hasPermission) {
                            twirk.channelMessage(commandDb.editCommand(id, content, permission));
                        }
                        else if (hasContent) {
                            twirk.channelMessage(commandDb.editMessage(id, content));
                        }
                        else if (hasPermission) {
                            twirk.channelMessage(commandDb.editPermission(id, permission));
                        }
                        else {
                            showError("no content or permission");
                        }
                        break;
                    case DELETE:
                        twirk.channelMessage(commandDb.deleteMessage(id));
                        break;
                }
        }
    }
    
    private void showError(String error) {
        twirk.channelMessage(String.format("ERROR: %s", error));
    }
    
    private TYPE getType(String type) {
        switch (type) {
            case "scheduled":
                return TYPE.SCHEDULED;
            case "command":
                return TYPE.COMMAND;
            default:
                return null;
        }
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
    
    private boolean permissionIsValid(String permission) {
        for (String value : validPermissions) {
            if (permission.toLowerCase().equals(value)) {
                return true;
            }
        }
        return false;
    }
}
