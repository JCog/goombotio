package listeners.commands;

import com.gikk.twirk.types.twitchMessage.TwitchMessage;
import com.gikk.twirk.types.users.TwitchUser;
import com.github.twitch4j.helix.domain.User;
import com.jcog.utils.TwitchApi;
import com.jcog.utils.TwitchUserLevel;
import com.jcog.utils.database.DbManager;
import com.jcog.utils.database.entries.CommandItem;
import com.jcog.utils.database.misc.CommandDb;
import util.CommandParser;
import util.TwirkInterface;

import java.util.HashSet;
import java.util.TimerTask;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class GenericCommandListener extends CommandBase {

    private final static String PATTERN = "";

    private final CommandDb commandDb;
    private final TwirkInterface twirk;
    private final CommandParser commandParser;
    private final HashSet<String> activeCooldowns = new HashSet<>();


    public GenericCommandListener(
            ScheduledExecutorService scheduler,
            TwirkInterface twirk,
            DbManager dbManager,
            TwitchApi twitchApi,
            User streamerUser
    ) {
        super(CommandType.GENERIC_COMMAND, scheduler);
        this.twirk = twirk;
        this.commandParser = new CommandParser(dbManager, twitchApi, streamerUser);
        commandDb = dbManager.getCommandDb();
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
        CommandItem commandItem = commandDb.getCommandItem(command);
        if (message.getContent().matches(".*\\$\\(.*\\).*")) {
            System.out.println(String.format(
                    "Ignoring user (%s) attempt at custom variable: %s",
                    sender.getDisplayName(),
                    message.getContent()
            ));
            return;
        }
        if (commandItem != null && userHasPermission(sender, commandItem) && !cooldownActive(commandItem)) {
            twirk.channelMessage(commandParser.parse(commandItem, sender, message));
            startCooldown(commandItem);
        }
    }

    private boolean userHasPermission(TwitchUser sender, CommandItem commandItem) {
        return TwitchUserLevel.getUserLevel(sender).value >= commandItem.getPermission().value;
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
}
