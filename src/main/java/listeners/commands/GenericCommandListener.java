package listeners.commands;

import com.github.twitch4j.chat.events.channel.ChannelMessageEvent;
import database.misc.CommandDb;
import listeners.TwitchEventListener;
import util.CommonUtils;
import util.MessageExpressionParser;
import util.TwitchApi;
import util.TwitchUserLevel;
import util.TwitchUserLevel.USER_LEVEL;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Locale;
import java.util.Set;

import static database.misc.CommandDb.CommandItem;

public class GenericCommandListener implements TwitchEventListener {
    private final CommandDb commandDb;
    private final HashMap<String, Instant> commandInstants;
    private final MessageExpressionParser commandParser;
    private final TwitchApi twitchApi;


    public GenericCommandListener(CommonUtils commonUtils, MessageExpressionParser commandParser) {
        this.commandParser = commandParser;
        twitchApi = commonUtils.getTwitchApi();
        commandDb = commonUtils.getDbManager().getCommandDb();
        commandInstants = new HashMap<>();
    }

    @Override
    public void onChannelMessage(ChannelMessageEvent messageEvent) {
        String content = messageEvent.getMessage().toLowerCase(Locale.ENGLISH).trim();
        String command = content.split("\\s", 2)[0];
        
        // on the off chance there is a reserved command also in the DB, this will prevent the DB one from running
        if (isReservedCommand(command)) {
            return;
        }
        
        Set<String> badges = messageEvent.getMessageEvent().getBadges().keySet();
        USER_LEVEL userLevel = TwitchUserLevel.getUserLevel(badges);
        CommandItem commandItem = commandDb.getCommandItem(command);
        if (commandItem == null || cooldownActive(commandItem) || !userHasPermission(userLevel, commandItem)) {
            return;
        }
    
        // TODO: replace this with code that actually escapes user input properly
        if (messageEvent.getMessage().matches(".*[()].*")) {
            String displayName = TwitchEventListener.getDisplayName(messageEvent.getMessageEvent());
            twitchApi.channelMessage(String.format(
                    "@%s Please don't use parentheses when using commands.",
                    displayName
            ));
            return;
        }
        
        twitchApi.channelMessage(commandParser.parse(commandItem, messageEvent));
        commandInstants.put(command, Instant.now());
    }

    private boolean userHasPermission(USER_LEVEL userLevel, CommandItem commandItem) {
        return userLevel.value >= commandItem.getPermission().value;
    }

    private boolean cooldownActive(CommandItem commandItem) {
        Instant lastUsed = commandInstants.get(commandItem.getId());
        if (lastUsed == null) {
            return false;
        }
        return ChronoUnit.SECONDS.between(lastUsed, Instant.now()) < commandItem.getCooldown();
    }

    private boolean isReservedCommand(String commandId) {
        return CommandBase.getReservedCommands().contains(commandId);
    }
}
