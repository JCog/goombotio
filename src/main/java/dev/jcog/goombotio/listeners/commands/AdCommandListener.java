package dev.jcog.goombotio.listeners.commands;

import com.github.twitch4j.chat.events.channel.ChannelMessageEvent;
import com.github.twitch4j.helix.domain.AdSchedule;
import com.netflix.hystrix.exception.HystrixRuntimeException;
import dev.jcog.goombotio.util.CommonUtils;
import dev.jcog.goombotio.util.TwitchApi;
import dev.jcog.goombotio.util.TwitchUserLevel.USER_LEVEL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class AdCommandListener extends CommandBase {
    private static final Logger log = LoggerFactory.getLogger(AdCommandListener.class);
    private static final CommandType COMMAND_TYPE = CommandType.PREFIX_COMMAND;
    private static final USER_LEVEL MIN_USER_LEVEL = USER_LEVEL.DEFAULT;
    private static final int COOLDOWN = 2;
    private static final CooldownType COOLDOWN_TYPE = CooldownType.GLOBAL;
    private static final String PATTERN = "!ad";

    private final TwitchApi twitchApi;

    public AdCommandListener(CommonUtils commonUtils) {
        super(COMMAND_TYPE, MIN_USER_LEVEL, COOLDOWN, COOLDOWN_TYPE, PATTERN);
        twitchApi = commonUtils.twitchApi();
    }

    @Override
    protected void performCommand(String command, USER_LEVEL userLevel, ChannelMessageEvent messageEvent) {
        AdSchedule adSchedule;
        try {
            adSchedule = twitchApi.getAdSchedule();
        } catch (HystrixRuntimeException e) {
            log.error(e.getMessage());
            twitchApi.channelMessage("Error retrieving ad schedule");
            return;
        }
        if (adSchedule == null) {
            twitchApi.channelMessage("There are currently no ads scheduled.");
            return;
        }

        Instant nextAdInstant = adSchedule.getNextAdAt();
        long minutesLeft = ChronoUnit.MINUTES.between(Instant.now(), nextAdInstant);
        if (minutesLeft < 0) {
            twitchApi.channelMessage("There are currently no ads scheduled.");
        } else if (minutesLeft == 0) {
            twitchApi.channelMessage("The next ad will run at any moment now.");
        } else {
            twitchApi.channelMessage(String.format(
                    "The next ad is scheduled to run in about %d minutes.",
                    minutesLeft + 1
            ));
        }
    }
}
