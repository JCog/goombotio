package listeners.commands;

import com.github.twitch4j.chat.events.channel.ChannelMessageEvent;
import database.DbManager;
import database.misc.CommandDb;
import listeners.TwitchEventListener;
import util.MessageExpressionParser;
import util.TwitchApi;
import util.TwitchUserLevel.USER_LEVEL;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.concurrent.ScheduledExecutorService;

import static database.misc.CommandDb.CommandItem;

public class GenericCommandListener extends CommandBase {
    private static final CommandType COMMAND_TYPE = CommandType.GENERIC_COMMAND;
    private static final USER_LEVEL MIN_USER_LEVEL = USER_LEVEL.DEFAULT;
    private static final int COOLDOWN = 0;
    private static final CooldownType COOLDOWN_TYPE = CooldownType.COMBINED;
    private static final String PATTERN = "";

    private final CommandDb commandDb;
    private final HashMap<String, Instant> commandInstants;
    private final MessageExpressionParser commandParser;
    private final TwitchApi twitchApi;


    public GenericCommandListener(
            ScheduledExecutorService scheduler,
            MessageExpressionParser commandParser,
            DbManager dbManager,
            TwitchApi twitchApi
    ) {
        super(scheduler, COMMAND_TYPE, MIN_USER_LEVEL, COOLDOWN, COOLDOWN_TYPE, PATTERN);
        this.commandParser = commandParser;
        this.twitchApi = twitchApi;
        commandDb = dbManager.getCommandDb();
        commandInstants = new HashMap<>();
    }

    @Override
    protected void performCommand(String command, USER_LEVEL userLevel, ChannelMessageEvent messageEvent) {
        // on the off chance there is a reserved command also in the DB, this will prevent the DB one from running
        if (isReservedCommand(command)) {
            return;
        }
        
        if (messageEvent.getMessage().matches(".*[()].*")) {
            String displayName = TwitchEventListener.getDisplayName(messageEvent);
            twitchApi.channelMessage(String.format(
                    "@%s Please don't use parentheses when using commands.",
                    displayName
            ));
            return;
        }

        CommandItem commandItem = commandDb.getCommandItem(command);
        if (commandItem == null || cooldownActive(commandItem) || !userHasPermission(userLevel, commandItem)) {
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
