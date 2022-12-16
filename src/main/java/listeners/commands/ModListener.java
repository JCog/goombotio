package listeners.commands;

import com.github.twitch4j.chat.events.channel.ChannelMessageEvent;
import util.TwitchApi;
import util.TwitchUserLevel.USER_LEVEL;

import java.util.concurrent.ScheduledExecutorService;

public class ModListener extends CommandBase {
    private static final CommandType COMMAND_TYPE = CommandType.CONTENT_COMMAND;
    private static final USER_LEVEL MIN_USER_LEVEL = USER_LEVEL.DEFAULT;
    private static final int COOLDOWN = 0;
    private static final String PATTERN = "crashes paper mario";
    
    private final TwitchApi twitchApi;

    public ModListener(ScheduledExecutorService scheduler, TwitchApi twitchApi) {
        super(scheduler, COMMAND_TYPE, MIN_USER_LEVEL, COOLDOWN, PATTERN);
        this.twitchApi = twitchApi;
    }

    @Override
    protected void performCommand(String command, USER_LEVEL userLevel, ChannelMessageEvent messageEvent) {
        twitchApi.channelMessage(String.format("/timeout %s 1", messageEvent.getUser().getName()));
    }
}
