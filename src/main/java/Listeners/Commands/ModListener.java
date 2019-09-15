package Listeners.Commands;

import com.gikk.twirk.Twirk;
import com.gikk.twirk.enums.USER_TYPE;
import com.gikk.twirk.types.twitchMessage.TwitchMessage;
import com.gikk.twirk.types.users.TwitchUser;

public class ModListener extends CommandBase {

    private final static String pattern = "RESET";
    private final Twirk twirk;

    public ModListener(Twirk twirk) {
        super(CommandType.EXACT_MATCH_COMMAND);
        this.twirk = twirk;
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
        twirk.channelMessage("Don't be toxic WhatsHisFace");
    }
}
