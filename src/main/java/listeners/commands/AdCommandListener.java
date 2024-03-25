package listeners.commands;

import com.github.twitch4j.chat.events.channel.ChannelMessageEvent;
import com.github.twitch4j.helix.domain.AdSchedule;
import util.TwitchApi;
import util.TwitchUserLevel.USER_LEVEL;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class AdCommandListener extends CommandBase {
    private static final CommandType COMMAND_TYPE = CommandType.PREFIX_COMMAND;
    private static final USER_LEVEL MIN_USER_LEVEL = USER_LEVEL.DEFAULT;
    private static final int COOLDOWN = 2;
    private static final CooldownType COOLDOWN_TYPE = CooldownType.COMBINED;
    private static final String PATTERN = "!ad";
    
    private final TwitchApi twitchApi;

    public AdCommandListener(TwitchApi twitchApi) {
        super(COMMAND_TYPE, MIN_USER_LEVEL, COOLDOWN, COOLDOWN_TYPE, PATTERN);
        this.twitchApi = twitchApi;
    }

    @Override
    protected void performCommand(String command, USER_LEVEL userLevel, ChannelMessageEvent messageEvent) {
        List<AdSchedule> adScheduleList = twitchApi.getAdSchedule();
        if (adScheduleList.isEmpty()) {
            twitchApi.channelMessage("There are currently no ads scheduled.");
            return;
        }
        
        AdSchedule adSchedule = adScheduleList.get(0);
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
