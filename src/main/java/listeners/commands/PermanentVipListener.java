package listeners.commands;

import com.github.twitch4j.chat.events.channel.ChannelMessageEvent;
import com.github.twitch4j.helix.domain.User;
import com.netflix.hystrix.exception.HystrixRuntimeException;
import database.DbManager;
import database.misc.PermanentVipsDb;
import util.TwitchApi;
import util.TwitchUserLevel.USER_LEVEL;

import java.util.concurrent.ScheduledExecutorService;

public class PermanentVipListener extends CommandBase {

    private final static String PATTERN_ADD = "!vipadd";
    private final static String PATTERN_DELETE = "!vipdelete";
    
    private final TwitchApi twitchApi;
    private final PermanentVipsDb permanentVipsDb;

    public PermanentVipListener(ScheduledExecutorService scheduler, TwitchApi twitchApi, DbManager dbManager) {
        super(scheduler, CommandType.PREFIX_COMMAND, USER_LEVEL.BROADCASTER, 0, PATTERN_ADD, PATTERN_DELETE);
        this.twitchApi = twitchApi;
        this.permanentVipsDb = dbManager.getPermanentVipsDb();
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
            e.printStackTrace();
            System.out.println("Error fetching user from Twitch API");
            twitchApi.channelMessage("ERROR: Twitch API error fetching user");
            return;
        }
        
        if (user == null) {
            twitchApi.channelMessage(String.format("ERROR: unable to find Twitch user \"%s\"", splitMessage[1]));
            return;
        }
    
        switch (command) {
            case PATTERN_ADD: {
                permanentVipsDb.addVip(user.getId());
                twitchApi.channelMessage(String.format("%s added to permanent VIP list", user.getDisplayName()));
                break;
            }
            case PATTERN_DELETE: {
                if (permanentVipsDb.deleteVip(user.getId())) {
                    twitchApi.channelMessage(String.format("%s removed from permanent VIP list", user.getDisplayName()));
                } else {
                    twitchApi.channelMessage(String.format("%s not found in permanent VIP list", user.getDisplayName()));
                }
                
            }
        }
    }
}
