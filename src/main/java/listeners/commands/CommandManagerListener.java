package listeners.commands;

import com.github.twitch4j.chat.events.channel.ChannelMessageEvent;
import database.misc.CommandDb;
import database.misc.CommandDb.CommandItem;
import org.apache.commons.cli.*;
import util.CommonUtils;
import util.TwitchApi;
import util.TwitchUserLevel;

import java.util.Iterator;

import static util.TwitchUserLevel.USER_LEVEL;

public class CommandManagerListener extends CommandBase {
    private static final String OPT_COOLDOWN = "cooldown";
    private static final String OPT_USER_LEVEL = "userlevel";
    private static final CommandType COMMAND_TYPE = CommandType.PREFIX_COMMAND;
    private static final USER_LEVEL MIN_USER_LEVEL = USER_LEVEL.DEFAULT;
    private static final int MANAGER_COOLDOWN = 0;
    private static final CooldownType COOLDOWN_TYPE = CooldownType.GLOBAL;
    private final static String PATTERN_ADD = "!addcom";
    private final static String PATTERN_EDIT = "!editcom";
    private final static String PATTERN_DELETE = "!delcom";
    private final static String PATTERN_ADD_ALIAS = "!addalias";
    private final static String PATTERN_DEL_ALIAS = "!delalias";
    private final static String PATTERN_DETAILS = "!comdetails";
    
    private final static long DEFAULT_COOLDOWN = 2; // seconds

    private final CommandDb commandDb;
    private final TwitchApi twitchApi;
    private final Options options = new Options();
    private final CommandLineParser parser = new DefaultParser();

    public CommandManagerListener(CommonUtils commonUtils) {
        super(
                COMMAND_TYPE,
                MIN_USER_LEVEL,
                MANAGER_COOLDOWN,
                COOLDOWN_TYPE,
                PATTERN_ADD,
                PATTERN_EDIT,
                PATTERN_DELETE,
                PATTERN_ADD_ALIAS,
                PATTERN_DEL_ALIAS,
                PATTERN_DETAILS
        );
        twitchApi = commonUtils.twitchApi();
        commandDb = commonUtils.dbManager().getCommandDb();
        options.addOption(Option.builder("c")
                .longOpt(OPT_COOLDOWN)
                .hasArg()
                .build()
        );
        options.addOption(Option.builder("l")
                .longOpt(OPT_USER_LEVEL)
                .hasArg()
                .build()
        );
    }

    @Override
    protected void performCommand(String command, USER_LEVEL commandUserLevel, ChannelMessageEvent messageEvent) {
        if (commandUserLevel.value < USER_LEVEL.MOD.value) {
            return;
        }
        
        String[] messageSplit = messageEvent.getMessage().split("\\s", 2);
        if (messageSplit.length < 2) {
            twitchApi.channelMessage("ERROR: missing arguments");
            return;
        }
    
        switch (command) {
            case PATTERN_ADD_ALIAS -> {
                String[] arguments = messageSplit[1].split("\\s");
                if (arguments.length != 2) {
                    twitchApi.channelMessage("ERROR: input should contain exactly two arguments");
                    return;
                }
                twitchApi.channelMessage(commandDb.addAlias(arguments[0], arguments[1]));
                return;
            }
            case PATTERN_DEL_ALIAS -> {
                String[] arguments = messageSplit[1].split("\\s");
                if (arguments.length != 1) {
                    twitchApi.channelMessage("ERROR: input should contain exactly one argument");
                    return;
                }
                twitchApi.channelMessage(commandDb.deleteAlias(arguments[0]));
                return;
            }
        }
        
        
        CommandLine parsed;
        try {
            parsed = parser.parse(options, messageSplit[1].split("\\s"));
        } catch (ParseException e) {
            twitchApi.channelMessage("ERROR: unable to parse input");
            return;
        }
        if (parsed.getArgList().isEmpty()) {
            twitchApi.channelMessage("ERROR: missing command name");
            return;
        }
    
        String commandId = parsed.getArgList().get(0);
        String message = null;
        Long cooldown = null;
        USER_LEVEL userLevel = null;
        
        Iterator<String> iterator = parsed.getArgList().iterator();
        iterator.next();
        StringBuilder builtMessage = new StringBuilder();
        while (iterator.hasNext()) {
            builtMessage.append(iterator.next());
            if (iterator.hasNext()) {
                builtMessage.append(" ");
            }
        }
        if (builtMessage.length() != 0) {
            message = builtMessage.toString();
        }
        if (message != null) {
            if (message.length() < 3 || message.charAt(0) != '\"' || message.charAt(message.length() - 1) != '\"') {
                twitchApi.channelMessage("ERROR: message must be enclosed in quotation marks");
                return;
            }
            message = message.substring(1, message.length() - 1);
        }
    
        if (parsed.hasOption(OPT_COOLDOWN)) {
            try {
                cooldown = Long.parseLong(parsed.getOptionValue(OPT_COOLDOWN));
            } catch (NumberFormatException e) {
                twitchApi.channelMessage("ERROR: invalid cooldown value");
                return;
            }
        }
    
        if (parsed.hasOption(OPT_USER_LEVEL)) {
            userLevel = TwitchUserLevel.getUserLevel(parsed.getOptionValue(OPT_USER_LEVEL));
            if (userLevel == null) {
                twitchApi.channelMessage("ERROR: invalid userlevel");
                return;
            }
        }
    
        switch (command) {
            case PATTERN_ADD -> {
                if (message == null) {
                    twitchApi.channelMessage("ERROR: no content");
                    return;
                }
                twitchApi.channelMessage(commandDb.addCommand(
                        commandId,
                        message,
                        cooldown == null ? DEFAULT_COOLDOWN : cooldown,
                        userLevel == null ? USER_LEVEL.DEFAULT : userLevel,
                        0
                ));
            }
            case PATTERN_EDIT -> {
                if (message == null && cooldown == null && userLevel == null) {
                    twitchApi.channelMessage("ERROR: nothing to edit");
                    return;
                }
                twitchApi.channelMessage(commandDb.editCommand(commandId, message, cooldown, userLevel));
            }
            case PATTERN_DELETE -> twitchApi.channelMessage(commandDb.deleteCommand(commandId));
            case PATTERN_DETAILS -> {
                CommandItem commandItem = commandDb.getCommandItem(commandId);
                if (commandItem == null) {
                    twitchApi.channelMessage(String.format("Unknown command \"%s\"", commandId));
                    return;
                }
                twitchApi.channelMessage(commandItem.toString());
            }
        }
    }
}
