package Listeners.Commands;

import Functions.StreamInfo;
import Util.CommandParser;
import Util.Database.CommandDb;
import Util.Database.Entries.CommandItem;
import Util.Settings;
import Util.TwirkInterface;
import Util.TwitchUserLevel;
import com.gikk.twirk.types.twitchMessage.TwitchMessage;
import com.gikk.twirk.types.users.TwitchUser;
import com.github.twitch4j.TwitchClient;

import java.util.HashSet;
import java.util.Timer;
import java.util.TimerTask;

public class GenericCommandListener extends CommandBase {

    private final static String PATTERN = "";
    
    private final TwirkInterface twirk;
    private final CommandDb commandDb;
    private final CommandParser commandParser;
    private final HashSet<String> activeCooldowns;
    

    public GenericCommandListener(TwirkInterface twirk, TwitchClient twitchClient, StreamInfo streamInfo) {
        super(CommandType.GENERIC_COMMAND);
        this.twirk = twirk;
        this.commandDb = CommandDb.getInstance();
        this.commandParser = new CommandParser(Settings.getTwitchAuthToken(), twitchClient, streamInfo);
        activeCooldowns = new HashSet<>();
    }

    @Override
    protected String getCommandWords() {
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
        CommandItem commandItem = commandDb.getCommandItem(command);
        if (commandItem != null && userHasPermission(sender, commandItem) && !cooldownActive(commandItem)) {
            twirk.channelMessage(commandParser.parse(commandItem, sender, message));
            startCooldown(commandItem);
        }
    }
    
    private boolean userHasPermission(TwitchUser sender, CommandItem commandItem) {
        return TwitchUserLevel.getUserLevel(sender).value >= commandItem.getPermission().value;
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
