package listeners.commands;

import com.gikk.twirk.types.twitchMessage.TwitchMessage;
import com.gikk.twirk.types.users.TwitchUser;
import database.DbManager;
import database.misc.BitWarDb;
import util.TwirkInterface;
import util.TwitchUserLevel;

import java.util.ArrayList;
import java.util.concurrent.ScheduledExecutorService;

public class BitWarResetListener extends CommandBase {
    private static final String PATTERN = "!resetyoshi";
    private static final String MESSAGE = "The Yoshi Bit War has been reset.";
    private static final String BIT_WAR_NAME = "save_kill_yoshi";
    private static final String TEAM_KILL = "team_kill";
    private static final String TEAM_SAVE = "team_save";

    private final TwirkInterface twirk;
    private final BitWarDb bitWarDb;

    public BitWarResetListener(ScheduledExecutorService scheduler, TwirkInterface twirk, DbManager dbManager) {
        super(CommandType.PREFIX_COMMAND, scheduler);
        this.twirk = twirk;
        this.bitWarDb = dbManager.getBitWarDb();
    }

    @Override
    public String getCommandWords() {
        return PATTERN;
    }

    @Override
    protected TwitchUserLevel.USER_LEVEL getMinUserPrivilege() {
        return TwitchUserLevel.USER_LEVEL.BROADCASTER;
    }

    @Override
    protected int getCooldownLength() {
        return 0;
    }

    @Override
    protected void performCommand(String command, TwitchUser sender, TwitchMessage message) {
        ArrayList<String> teams = new ArrayList<>();
        teams.add(TEAM_KILL);
        teams.add(TEAM_SAVE);
        bitWarDb.resetBitWar(BIT_WAR_NAME, teams);
        twirk.channelMessage(MESSAGE);
    }
}
