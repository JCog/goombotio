package listeners.commands;

import com.github.twitch4j.chat.events.channel.ChannelMessageEvent;
import util.TwitchApi;
import util.TwitchUserLevel.USER_LEVEL;

import java.util.concurrent.ScheduledExecutorService;

public class ModListener extends CommandBase {

    private final static String PATTERN = "crashes paper mario";
    private final TwitchApi twitchApi;

    public ModListener(ScheduledExecutorService scheduler, TwitchApi twitchApi) {
        super(scheduler, CommandType.CONTENT_COMMAND, USER_LEVEL.DEFAULT, 0, PATTERN);
        this.twitchApi = twitchApi;
    }

    @Override
    protected void performCommand(String command, USER_LEVEL userLevel, ChannelMessageEvent messageEvent) {
        twitchApi.channelMessage(String.format("/timeout %s 1", messageEvent.getUser().getName()));
    }
}
