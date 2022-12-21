package listeners.commands;

import com.github.twitch4j.chat.events.channel.ChannelMessageEvent;
import listeners.TwitchEventListener;
import util.TwitchUserLevel;
import util.TwitchUserLevel.USER_LEVEL;

import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public abstract class CommandBase implements TwitchEventListener {
    private static final char GENERIC_COMMAND_CHAR = '!';
    private static final Set<String> reservedCommands = new HashSet<>();

    /*
    PREFIX_COMMAND: command (first "word") matches exactly
    CONTENT_COMMAND: message contains pattern
    EXACT_MATCH_COMMAND: entire message matches pattern (case-sensitive)
    GENERIC_COMMAND: message begins with '!'
     */
    protected enum CommandType {
        PREFIX_COMMAND,
        CONTENT_COMMAND,
        EXACT_MATCH_COMMAND,
        GENERIC_COMMAND
    }
    
    final ScheduledExecutorService scheduler;
    
    private final CommandType commandType;
    private final USER_LEVEL minUserLevel;
    private final int cooldownLength;
    private final Set<String> commandPatterns;

    private boolean coolingDown;

    protected CommandBase(
            ScheduledExecutorService scheduler,
            CommandType commandType,
            USER_LEVEL minUserLevel,
            int cooldownLength,
            String ... commandPatterns
    ) {
        this.scheduler = scheduler;
        this.commandType = commandType;
        this.minUserLevel = minUserLevel;
        this.cooldownLength = cooldownLength;
        this.commandPatterns = compileCommandPattern(commandPatterns);
        reservedCommands.addAll(List.of(commandPatterns));
        coolingDown = false;
    }
    
    public static Set<String> getReservedCommands() {
        return reservedCommands;
    }

    @Override
    public void onChannelMessage(ChannelMessageEvent messageEvent) {
        String exactContent = messageEvent.getMessage().trim();
        if (exactContent.length() == 0) {
            return;
        }
        String content = messageEvent.getMessage().toLowerCase(Locale.ENGLISH).trim();
        String[] split = content.split("\\s", 2);
        String command = split[0];
        char firstChar = content.charAt(0);
        Set<String> badges = messageEvent.getMessageEvent().getBadges().keySet();
        USER_LEVEL userLevel = TwitchUserLevel.getUserLevel(badges);
    
        if (!coolingDown || userLevel.value == USER_LEVEL.BROADCASTER.value) {
            if (userLevel.value >= minUserLevel.value) {
                switch (commandType) {
                    case PREFIX_COMMAND:
                        for (String pattern : commandPatterns) {
                            if (command.equals(pattern)) {
                                performCommand(pattern, userLevel, messageEvent);
                                startCooldown();
                                break;    //We don't want to fire twice for the same message
                            }
                        }
                        break;
        
                    case CONTENT_COMMAND:
                        for (String pattern : commandPatterns) {
                            if (content.contains(pattern)) {
                                performCommand(pattern, userLevel, messageEvent);
                                startCooldown();
                                break;
                            }
                        }
                        break;
        
                    case EXACT_MATCH_COMMAND:
                        for (String pattern : commandPatterns) {
                            if (exactContent.equals(pattern)) {
                                performCommand(pattern, userLevel, messageEvent);
                                startCooldown();
                                break;
                            }
                        }
                        break;
                    case GENERIC_COMMAND:
                        if (firstChar == GENERIC_COMMAND_CHAR) {
                            performCommand(command, userLevel, messageEvent);
                        }
                        break;
                }
            }
        }
    }
    
    private Set<String> compileCommandPattern(String[] commandWords) {
        Set<String> out = new HashSet<>();
        Collections.addAll(out, commandWords);
        return out;
    }

    private void startCooldown() {
        if (cooldownLength == 0) {
            return;
        }
        coolingDown = true;
        scheduler.schedule(() -> {
            coolingDown = false;
        }, cooldownLength, TimeUnit.MILLISECONDS);
    }
    
    protected abstract void performCommand(String command, USER_LEVEL userLevel, ChannelMessageEvent messageEvent);
}
