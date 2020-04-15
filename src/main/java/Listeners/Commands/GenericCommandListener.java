package Listeners.Commands;

import Util.Database.CommandDb;
import Util.Database.Entries.CommandItem;
import com.gikk.twirk.Twirk;
import com.gikk.twirk.enums.USER_TYPE;
import com.gikk.twirk.types.twitchMessage.TwitchMessage;
import com.gikk.twirk.types.users.TwitchUser;

import java.util.HashSet;
import java.util.Timer;
import java.util.TimerTask;

public class GenericCommandListener extends CommandBase {

    private final static String PATTERN = "";
    
    private final Twirk twirk;
    private final CommandDb commandDb;
    private final HashSet<String> activeCooldowns;
    

    public GenericCommandListener(Twirk twirk) {
        super(CommandType.GENERIC_COMMAND);
        this.twirk = twirk;
        this.commandDb = CommandDb.getInstance();
        activeCooldowns = new HashSet<>();
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
        CommandItem commandItem = commandDb.getCommandItem(command);
        if (commandItem != null && userHasPermission(sender, commandItem) && !cooldownActive(commandItem)) {
            twirk.channelMessage(commandItem.getMessage());
            startCooldown(commandItem);
        }
    }
    
    private boolean userHasPermission(TwitchUser sender, CommandItem commandItem) {
        return sender.getUserType().value >= commandItem.getPermission().value;
    }
    
    private void startCooldown(CommandItem commandItem) {
        if(activeCooldowns.contains(commandItem.getId())) {
            return;
        }
        
        activeCooldowns.add(commandItem.getId());
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                activeCooldowns.remove(commandItem.getId());
            }
        }, commandItem.getCooldown());
    }
    
    private boolean cooldownActive(CommandItem commandItem) {
        return activeCooldowns.contains(commandItem.getId());
    }
}
