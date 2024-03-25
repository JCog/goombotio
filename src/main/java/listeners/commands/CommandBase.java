package listeners.commands;

import com.github.twitch4j.chat.events.channel.ChannelMessageEvent;
import listeners.TwitchEventListener;
import util.TwitchUserLevel;
import util.TwitchUserLevel.USER_LEVEL;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

public abstract class CommandBase implements TwitchEventListener {
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
        EXACT_MATCH_COMMAND
    }
    
    protected enum CooldownType {
        PER_USER,
        COMBINED
    }
    
    private final CommandType commandType;
    private final USER_LEVEL minUserLevel;
    private final Set<String> commandPatterns;
    private final int cooldownLength; // seconds
    private final CooldownType cooldownType;
    private final HashMap<String, Instant> recentUsages;
    
    private Instant lastUsed;

    protected CommandBase(
            CommandType commandType,
            USER_LEVEL minUserLevel,
            int cooldownLength,
            CooldownType cooldownType,
            String ... commandPatterns
    ) {
        this.commandType = commandType;
        this.minUserLevel = minUserLevel;
        this.cooldownLength = cooldownLength;
        this.cooldownType = cooldownType;
        this.commandPatterns = compileCommandPattern(commandPatterns);
        recentUsages = new HashMap<>();
        reservedCommands.addAll(List.of(commandPatterns));
        lastUsed = Instant.MIN;
    }
    
    public static Set<String> getReservedCommands() {
        return reservedCommands;
    }

    @Override
    public void onChannelMessage(ChannelMessageEvent messageEvent) {
        String exactContent = messageEvent.getMessage().trim();
        if (exactContent.isEmpty()) {
            return;
        }
        
        switch (cooldownType) {
            case PER_USER:
                String userId = messageEvent.getUser().getId();
                Instant userInstant = recentUsages.get(userId);
                if (userInstant != null && ChronoUnit.SECONDS.between(userInstant, Instant.now()) < cooldownLength) {
                    return;
                }
                break;
            case COMBINED:
                if (ChronoUnit.SECONDS.between(lastUsed, Instant.now()) < cooldownLength) {
                    return;
                }
                break;
        }
        
        String content = messageEvent.getMessage().toLowerCase(Locale.ENGLISH).trim();
        String command = content.split("\\s", 2)[0];
        Set<String> badges = messageEvent.getMessageEvent().getBadges().keySet();
        USER_LEVEL userLevel = TwitchUserLevel.getUserLevel(badges);
    
        if (userLevel.value >= minUserLevel.value) {
            switch (commandType) {
                case PREFIX_COMMAND:
                    for (String pattern : commandPatterns) {
                        if (command.equals(pattern)) {
                            performCommand(pattern, userLevel, messageEvent);
                            resetCooldown(messageEvent.getUser().getId());
                            break; // We don't want to fire twice for the same message
                        }
                    }
                    break;
    
                case CONTENT_COMMAND:
                    for (String pattern : commandPatterns) {
                        if (content.contains(pattern)) {
                            performCommand(pattern, userLevel, messageEvent);
                            resetCooldown(messageEvent.getUser().getId());
                            break;
                        }
                    }
                    break;
    
                case EXACT_MATCH_COMMAND:
                    for (String pattern : commandPatterns) {
                        if (exactContent.equals(pattern)) {
                            performCommand(pattern, userLevel, messageEvent);
                            resetCooldown(messageEvent.getUser().getId());
                            break;
                        }
                    }
                    break;
            }
        }
    }
    
    private void resetCooldown(String userId) {
        switch (cooldownType) {
            case PER_USER:
                recentUsages.put(userId, lastUsed);
                break;
            case COMBINED:
                lastUsed = Instant.now();
                break;
        }
    }
    
    private Set<String> compileCommandPattern(String[] commandWords) {
        Set<String> out = new HashSet<>();
        Collections.addAll(out, commandWords);
        return out;
    }
    
    protected abstract void performCommand(String command, USER_LEVEL userLevel, ChannelMessageEvent messageEvent);
}
