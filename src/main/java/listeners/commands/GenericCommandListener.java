package listeners.commands;

import com.github.twitch4j.chat.events.channel.ChannelMessageEvent;
import database.DbManager;
import database.misc.CommandDb;
import util.MessageExpressionParser;
import util.TwitchApi;
import util.TwitchUserLevel.USER_LEVEL;

import java.util.HashSet;
import java.util.TimerTask;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static database.misc.CommandDb.CommandItem;

public class GenericCommandListener extends CommandBase {
    private static final CommandType COMMAND_TYPE = CommandType.GENERIC_COMMAND;
    private static final USER_LEVEL MIN_USER_LEVEL = USER_LEVEL.DEFAULT;
    private static final int COOLDOWN = 0;
    private static final String PATTERN = "";

    private final CommandDb commandDb;
    private final MessageExpressionParser commandParser;
    private final TwitchApi twitchApi;
    private final HashSet<String> activeCooldowns = new HashSet<>();


    public GenericCommandListener(
            ScheduledExecutorService scheduler,
            MessageExpressionParser commandParser,
            DbManager dbManager,
            TwitchApi twitchApi
    ) {
        super(scheduler, COMMAND_TYPE, MIN_USER_LEVEL, COOLDOWN, PATTERN);
        this.commandParser = commandParser;
        this.twitchApi = twitchApi;
        commandDb = dbManager.getCommandDb();
    }

    @Override
    protected void performCommand(String command, USER_LEVEL userLevel, ChannelMessageEvent messageEvent) {
        if (messageEvent.getMessage().matches(".*\\$\\(.*\\).*") && !isReservedCommand(command)) {
            System.out.printf(
                    "Ignoring user (%s) attempt at custom variable: %s%n",
                    messageEvent.getUser().getName(),
                    messageEvent.getMessage()
            );
            return;
        }

        CommandItem commandItem = commandDb.getCommandItem(command);
        if (commandItem != null && userHasPermission(userLevel, commandItem) && !cooldownActive(commandItem)) {
            twitchApi.channelMessage(commandParser.parse(commandItem, messageEvent));
            startCooldown(commandItem);
        }
    }

    private boolean userHasPermission(USER_LEVEL userLevel, CommandItem commandItem) {
        return userLevel.value >= commandItem.getPermission().value;
    }

    private void startCooldown(CommandItem commandItem) {
        if (activeCooldowns.contains(commandItem.getId())) {
            return;
        }

        activeCooldowns.add(commandItem.getId());
        scheduler.schedule(new TimerTask() {
            @Override
            public void run() {
                activeCooldowns.remove(commandItem.getId());
            }
        }, commandItem.getCooldown(), TimeUnit.SECONDS);
    }

    private boolean cooldownActive(CommandItem commandItem) {
        return activeCooldowns.contains(commandItem.getId());
    }

    private boolean isReservedCommand(String commandId) {
        return CommandBase.getReservedCommands().contains(commandId);
    }
}
