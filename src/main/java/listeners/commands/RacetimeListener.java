package listeners.commands;

import api.racetime.RacetimeApi;
import com.github.twitch4j.chat.events.channel.ChannelMessageEvent;
import util.CommonUtils;
import util.TwitchApi;
import util.TwitchUserLevel.USER_LEVEL;

import java.util.Objects;

public class RacetimeListener extends CommandBase {
    private static final CommandType COMMAND_TYPE = CommandType.PREFIX_COMMAND;
    private static final USER_LEVEL MIN_USER_LEVEL = USER_LEVEL.DEFAULT;
    private static final int COOLDOWN = 2;
    private static final CooldownType COOLDOWN_TYPE = CooldownType.GLOBAL;
    private static final String PATTERN = "!multi";
    private static final String GAME_SLUG = "pm64r";
    private static final String USERNAME = "JCog#3335";
    
    private final TwitchApi twitchApi;
    private final RacetimeApi racetimeApi;

    public RacetimeListener(CommonUtils commonUtils) {
        super(COMMAND_TYPE, MIN_USER_LEVEL, COOLDOWN, COOLDOWN_TYPE, PATTERN);
        twitchApi = commonUtils.twitchApi();
        racetimeApi = commonUtils.apiManager().getRacetimeApi();
    }

    @Override
    protected void performCommand(String command, USER_LEVEL userLevel, ChannelMessageEvent messageEvent) {
        String spectateUrl = racetimeApi.getSpectateUrl(USERNAME, GAME_SLUG);
        twitchApi.channelMessage(Objects.requireNonNullElse(
                spectateUrl,
                "There are currently no active races to spectate."
        ));
    }
}
