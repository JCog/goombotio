package Listeners.Commands;

import Util.Database.CommandDb;
import com.gikk.twirk.Twirk;
import com.gikk.twirk.enums.USER_TYPE;
import com.gikk.twirk.types.twitchMessage.TwitchMessage;
import com.gikk.twirk.types.users.TwitchUser;

public class GenericCommandListener extends CommandBase {

    private final static String pattern = "";
    private final Twirk twirk;
    private final CommandDb commandDb;
    

    public GenericCommandListener(Twirk twirk) {
        super(CommandType.GENERIC_COMMAND);
        this.twirk = twirk;
        this.commandDb = CommandDb.getInstance();
    }

    @Override
    protected String getCommandWords() {
        return pattern;
    }

    @Override
    protected USER_TYPE getMinUserPrivilege() {
        return USER_TYPE.DEFAULT;
    }

    @Override
    protected int getCooldownLength() {
        return 0;
    }

    @Override
    protected void performCommand(String command, TwitchUser sender, TwitchMessage message) {
        String output = commandDb.getMessage(command);
        if (output != null) {
            twirk.channelMessage(output);
        }
    }
}
