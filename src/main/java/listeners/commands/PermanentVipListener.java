package listeners.commands;

import com.github.twitch4j.chat.events.channel.ChannelMessageEvent;
import com.github.twitch4j.helix.domain.User;
import com.netflix.hystrix.exception.HystrixRuntimeException;
import database.misc.VipDb;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.CommonUtils;
import util.TwitchApi;
import util.TwitchUserLevel.USER_LEVEL;

public class PermanentVipListener extends CommandBase {
    private static final Logger log = LoggerFactory.getLogger(PermanentVipListener.class);
    private static final CommandType COMMAND_TYPE = CommandType.PREFIX_COMMAND;
    private static final USER_LEVEL MIN_USER_LEVEL = USER_LEVEL.BROADCASTER;
    private static final int COOLDOWN = 0;
    private static final CooldownType COOLDOWN_TYPE = CooldownType.GLOBAL;
    private static final String PATTERN_ADD = "!vipadd";
    private static final String PATTERN_DELETE = "!vipdelete";

    private final TwitchApi twitchApi;
    private final VipDb vipDb;

    public PermanentVipListener(CommonUtils commonUtils) {
        super(COMMAND_TYPE, MIN_USER_LEVEL, COOLDOWN, COOLDOWN_TYPE, PATTERN_ADD, PATTERN_DELETE);
        twitchApi = commonUtils.twitchApi();
        vipDb = commonUtils.dbManager().getVipDb();
    }

    @Override
    protected void performCommand(String command, USER_LEVEL userLevel, ChannelMessageEvent messageEvent) {
        String[] splitMessage = messageEvent.getMessage().trim().split("\\s");
        if (splitMessage.length < 2) {
            twitchApi.channelMessage("ERROR: no username provided");
            return;
        }
    
        User user;
        try {
            user = twitchApi.getUserByUsername(splitMessage[1]);
        } catch (HystrixRuntimeException e) {
            log.error("Error fetching user from Twitch API");
            twitchApi.channelMessage("ERROR: Twitch API error fetching user");
            return;
        }
        
        if (user == null) {
            twitchApi.channelMessage(String.format("ERROR: unable to find Twitch user \"%s\"", splitMessage[1]));
            return;
        }
    
        switch (command) {
            case PATTERN_ADD -> {
                vipDb.editPermanentProp(user.getId(), true);
                twitchApi.channelMessage(String.format("%s added to permanent VIP list", user.getDisplayName()));
            }
            case PATTERN_DELETE -> {
                vipDb.editPermanentProp(user.getId(), false);
                twitchApi.channelMessage(String.format("%s removed from permanent VIP list", user.getDisplayName()));
            }
        }
    }
}
