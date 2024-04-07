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
        twitchApi = commonUtils.twitchApi();
        commandDb = commonUtils.dbManager().getCommandDb();
        commandInstants = new HashMap<>();
    }

    @Override
    public void onChannelMessage(ChannelMessageEvent messageEvent) {
        String content = messageEvent.getMessage().toLowerCase(Locale.ENGLISH).trim();
        String[] split = content.split("\\s", 2);
        String command = split[0];
        String userInput = split.length > 1 ? split[1] : "";
        
        // on the off chance there is a reserved command also in the DB, this will prevent the DB one from running
        if (isReservedCommand(command)) {
            return;
        }
        
        Set<String> badges = messageEvent.getMessageEvent().getBadges().keySet();
        USER_LEVEL userLevel = TwitchUserLevel.getUserLevel(badges);
        CommandItem commandItem = commandDb.getCommandItem(command);
        if (commandItem == null || cooldownActive(command, commandItem.cooldown()) || !userHasPermission(userLevel, commandItem)) {
            return;
        }
        
        twitchApi.channelMessage(commandParser.parseCommandMessage(
                commandItem,
                userInput,
                messageEvent.getUser().getId(),
                TwitchEventListener.getDisplayName(messageEvent.getMessageEvent())
        ));
        commandInstants.put(command, Instant.now());
    }

    private boolean userHasPermission(USER_LEVEL userLevel, CommandItem commandItem) {
        return userLevel.value >= commandItem.permission().value;
    }

    private boolean cooldownActive(String command, long cooldown) {
        Instant lastUsed = commandInstants.get(command);
        if (lastUsed == null) {
            return false;
        }
        return ChronoUnit.SECONDS.between(lastUsed, Instant.now()) < cooldown;
    }

    private boolean isReservedCommand(String commandId) {
        return CommandBase.getReservedCommands().contains(commandId);
    }
}
