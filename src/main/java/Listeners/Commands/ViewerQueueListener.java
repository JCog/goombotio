package Listeners.Commands;

import Functions.ViewerQueueManager;
import com.gikk.twirk.Twirk;
import com.gikk.twirk.enums.USER_TYPE;
import com.gikk.twirk.types.twitchMessage.TwitchMessage;
import com.gikk.twirk.types.users.TwitchUser;

public class ViewerQueueListener extends CommandBase {
    
    private final static String pattern = "!join";
    private final Twirk twirk;
    private final ViewerQueueManager viewerQueueManager;
    
    public ViewerQueueListener(Twirk twirk, ViewerQueueManager viewerQueueManager) {
        super(CommandType.EXACT_MATCH_COMMAND);
        this.twirk = twirk;
        this.viewerQueueManager = viewerQueueManager;
    }
    
    @Override
    protected String getCommandWords() {
        return pattern;
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
