package listeners.commands;

import com.github.twitch4j.chat.events.channel.ChannelMessageEvent;
import database.DbManager;
import database.misc.BitWarDb;
import util.TwitchApi;
import util.TwitchUserLevel;

import java.util.ArrayList;
import java.util.concurrent.ScheduledExecutorService;

public class BitWarResetCommandListener extends CommandBase {
    private static final String PATTERN = "!resetyoshi";
    private static final String MESSAGE = "The Yoshi Bit War has been reset.";
    private static final String BIT_WAR_NAME = "save_kill_yoshi";
    private static final String TEAM_KILL = "team_kill";
    private static final String TEAM_SAVE = "team_save";

    private final TwitchApi twitchApi;
    private final BitWarDb bitWarDb;

    public BitWarResetCommandListener(ScheduledExecutorService scheduler, TwitchApi twitchApi, DbManager dbManager) {
        super(CommandType.PREFIX_COMMAND, scheduler);
        this.twitchApi = twitchApi;
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
    protected void performCommand(String command, TwitchUserLevel.USER_LEVEL userLevel, ChannelMessageEvent messageEvent) {
        ArrayList<String> teams = new ArrayList<>();
        teams.add(TEAM_KILL);
        teams.add(TEAM_SAVE);
        bitWarDb.resetBitWar(BIT_WAR_NAME, teams);
        twitchApi.channelMessage(MESSAGE);
    }
}