package listeners.commands;

import com.github.twitch4j.chat.events.channel.ChannelMessageEvent;
import database.DbManager;
import database.misc.BitWarDb;
import util.TwitchApi;
import util.TwitchUserLevel.USER_LEVEL;

import java.util.ArrayList;
import java.util.List;

public class BitWarResetCommandListener extends CommandBase {
    private static final CommandType COMMAND_TYPE = CommandType.PREFIX_COMMAND;
    private static final USER_LEVEL MIN_USER_LEVEL = USER_LEVEL.BROADCASTER;
    private static final int COOLDOWN = 0;
    private static final CooldownType COOLDOWN_TYPE = CooldownType.COMBINED;
    private static final String PATTERN = "!resetyoshi";
    
    private static final String MESSAGE = "The Yoshi Bit War has been reset.";
    private static final String BIT_WAR_NAME = "save_kill_yoshi";
    private static final String TEAM_KILL = "team_kill";
    private static final String TEAM_SAVE = "team_save";

    private final TwitchApi twitchApi;
    private final BitWarDb bitWarDb;

    public BitWarResetCommandListener(TwitchApi twitchApi, DbManager dbManager) {
        super(COMMAND_TYPE, MIN_USER_LEVEL, COOLDOWN, COOLDOWN_TYPE, PATTERN);
        this.twitchApi = twitchApi;
        this.bitWarDb = dbManager.getBitWarDb();
    }

    @Override
    protected void performCommand(String command, USER_LEVEL userLevel, ChannelMessageEvent messageEvent) {
        List<String> teams = new ArrayList<>();
        teams.add(TEAM_KILL);
        teams.add(TEAM_SAVE);
        bitWarDb.resetBitWar(BIT_WAR_NAME, teams);
        twitchApi.channelMessage(MESSAGE);
    }
}
