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
            twirk.channelMessage("ERROR: missing arguments");
            return;
        }
    
        FUNCTION function = getFunction(messageSplit[1]);
        if (function == null) {
            twirk.channelMessage(String.format("ERROR: Unknown function \"%s\"", messageSplit[1]));
            return;
        }
        
        TYPE type = getType(messageSplit[2]);
        if (type == null) {
            twirk.channelMessage(String.format("ERROR: Unknown type \"%s\"", messageSplit[2]));
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
            if (function != FUNCTION.DELETE) {
                twirk.channelMessage("ERROR: no content");
                return;
            }
        }
        
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
                        twirk.channelMessage(commandDb.addMessage(id, content));
                        break;
                    case EDIT:
                        twirk.channelMessage(commandDb.editMessage(id, content));
                        break;
                    case DELETE:
                        twirk.channelMessage(commandDb.deleteMessage(id));
                        break;
                }
        }
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
}
