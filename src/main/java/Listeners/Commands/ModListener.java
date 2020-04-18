package Listeners.Commands;

import Util.TwirkInterface;
import com.gikk.twirk.enums.USER_TYPE;
import com.gikk.twirk.types.twitchMessage.TwitchMessage;
import com.gikk.twirk.types.users.TwitchUser;

public class ModListener extends CommandBase {

    private final static String PATTERN = "crashes paper mario";
    private final TwirkInterface twirk;

    public ModListener(TwirkInterface twirk) {
        super(CommandType.CONTENT_COMMAND);
        this.twirk = twirk;
    }

    @Override
    protected String getCommandWords() {
        return PATTERN;
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
        twirk.channelMessage(String.format("/timeout %s 1", sender.getUserName()));
    }
}
