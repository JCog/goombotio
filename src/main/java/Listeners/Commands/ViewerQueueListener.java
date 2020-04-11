package Listeners.Commands;

import Functions.ViewerQueueManager;
import com.gikk.twirk.enums.USER_TYPE;
import com.gikk.twirk.types.twitchMessage.TwitchMessage;
import com.gikk.twirk.types.users.TwitchUser;

public class ViewerQueueListener extends CommandBase {
    
    private final static String PATTERN = "!join";
    
    private final ViewerQueueManager viewerQueueManager;
    
    public ViewerQueueListener(ViewerQueueManager viewerQueueManager) {
        super(CommandType.EXACT_MATCH_COMMAND);
        this.viewerQueueManager = viewerQueueManager;
    }
    
    @Override
    protected String getCommandWords() {
        return PATTERN;
    }
    
    @Override
    protected USER_TYPE getMinUserPrivilege() {
        return USER_TYPE.DEFAULT;
    }
    
    @Override
    protected int getCooldownLength() {
        return 0;
    }
    
    @Override
    protected void performCommand(String command, TwitchUser sender, TwitchMessage message) {
        viewerQueueManager.addViewer(sender);
    }
}
