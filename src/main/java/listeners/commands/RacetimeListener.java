package listeners.commands;

import api.RacetimeApi;
import com.github.twitch4j.chat.events.channel.ChannelMessageEvent;
import util.TwitchApi;
import util.TwitchUserLevel.USER_LEVEL;

import java.util.Objects;
import java.util.concurrent.ScheduledExecutorService;

public class RacetimeListener extends CommandBase {
    private static final CommandType COMMAND_TYPE = CommandType.PREFIX_COMMAND;
    private static final USER_LEVEL MIN_USER_LEVEL = USER_LEVEL.DEFAULT;
    private static final int COOLDOWN = 2 * 1000;
    private static final String PATTERN = "!multi";
    private static final String GAME_SLUG = "pm64r";
    private static final String USERNAME = "JCog#3335";
    
    private final TwitchApi twitchApi;

    public RacetimeListener(ScheduledExecutorService scheduler, TwitchApi twitchApi) {
        super(scheduler, COMMAND_TYPE, MIN_USER_LEVEL, COOLDOWN, PATTERN);
        this.twitchApi = twitchApi;
    }

    @Override
    protected void performCommand(String command, USER_LEVEL userLevel, ChannelMessageEvent messageEvent) {
        String spectateUrl = RacetimeApi.getSpectateUrl(USERNAME, GAME_SLUG);
        twitchApi.channelMessage(Objects.requireNonNullElse(
                spectateUrl,
                "There are currently no active races to spectate."
        ));
    }
}
