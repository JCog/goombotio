package dev.jcog.goombotio.listeners.commands;

import com.github.twitch4j.chat.events.channel.ChannelMessageEvent;
import dev.jcog.goombotio.util.CommonUtils;
import dev.jcog.goombotio.util.TwitchApi;
import dev.jcog.goombotio.util.TwitchUserLevel.USER_LEVEL;

public class MuteCommandListener extends CommandBase {
    private static final CommandType COMMAND_TYPE = CommandType.PREFIX_COMMAND;
    private static final USER_LEVEL MIN_USER_LEVEL = USER_LEVEL.BROADCASTER;
    private static final int COOLDOWN = 0;
    private static final CooldownType COOLDOWN_TYPE = CooldownType.GLOBAL;
    private static final String PATTERN = "!mute";
    
    private final TwitchApi twitchApi;

    public MuteCommandListener(CommonUtils commonUtils) {
        super(COMMAND_TYPE, MIN_USER_LEVEL, COOLDOWN, COOLDOWN_TYPE, PATTERN);
        twitchApi = commonUtils.twitchApi();
    }

    @Override
    protected void performCommand(String command, USER_LEVEL userLevel, ChannelMessageEvent messageEvent) {
        twitchApi.toggleSlientChat();
    }
}
