package Listeners.Commands;

import Functions.ViewerQueueManager;
import Util.TwitchUserLevel;
import com.gikk.twirk.types.twitchMessage.TwitchMessage;
import com.gikk.twirk.types.users.TwitchUser;

import java.util.concurrent.ScheduledExecutorService;

public class ViewerQueueJoinListener extends CommandBase {
    
    private final static String PATTERN = "!join";
    
    private final ViewerQueueManager viewerQueueManager;
    
    private boolean enabled;
    
    public ViewerQueueJoinListener(ScheduledExecutorService scheduler, ViewerQueueManager viewerQueueManager) {
        super(CommandType.EXACT_MATCH_COMMAND, scheduler);
        this.viewerQueueManager = viewerQueueManager;
        enabled = false;
    }
    
    public void start() {
        enabled = true;
    }
    
    public void stop() {
        enabled = false;
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
    protected void performCommand(String command, TwitchUser sender, TwitchMessage message) {
        if (enabled) {
            viewerQueueManager.addViewer(sender);
        }
    }
}
