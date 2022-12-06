package listeners.commands;

import com.github.twitch4j.chat.events.channel.ChannelMessageEvent;
import util.TwitchApi;
import util.TwitchUserLevel;

import java.util.concurrent.ScheduledExecutorService;

public class ModListener extends CommandBase {

    private final static String PATTERN = "crashes paper mario";
    private final TwitchApi twitchApi;

    public ModListener(ScheduledExecutorService scheduler, TwitchApi twitchApi) {
        super(CommandType.CONTENT_COMMAND, scheduler);
        this.twitchApi = twitchApi;
    }

    @Override
    public String getCommandWords() {
        return PATTERN;
    }

    @Override
    protected TwitchUserLevel.USER_LEVEL getMinUserPrivilege() {
        return TwitchUserLevel.USER_LEVEL.DEFAULT;
    }

    @Override
    protected int getCooldownLength() {
        return 0;
    }

    @Override
    protected void performCommand(String command, TwitchUserLevel.USER_LEVEL userLevel, ChannelMessageEvent messageEvent) {
        twitchApi.channelMessage(String.format("/timeout %s 1", messageEvent.getUser().getName()));
    }
}
