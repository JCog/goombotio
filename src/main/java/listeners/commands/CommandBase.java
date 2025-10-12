package listeners.commands;

import com.github.twitch4j.chat.events.channel.ChannelMessageEvent;
import listeners.TwitchEventListener;
import org.jetbrains.annotations.NotNull;
import util.TwitchUserLevel;
import util.TwitchUserLevel.USER_LEVEL;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;

public abstract class CommandBase implements TwitchEventListener {
    private static final Set<String> reservedCommands = new HashSet<>();

    /*
    PREFIX_COMMAND: command (first "word") matches exactly
    CONTENT_COMMAND: message contains pattern
    EXACT_MATCH_COMMAND: entire message matches pattern (case-sensitive)
     */
    protected enum CommandType {
        PREFIX_COMMAND,
        CONTENT_COMMAND,
        EXACT_MATCH_COMMAND
    }
    
    protected enum CooldownType {
        PER_USER,
        GLOBAL
    }
    
    private final CommandType commandType;
    private final USER_LEVEL minUserLevel;
    private final Set<String> commandPatterns;
    private final int cooldownLength; // seconds
    private final CooldownType cooldownType;
    private final HashMap<String, Instant> recentUsages;
    
    private Instant lastUsed;

    protected CommandBase(
            @NotNull CommandType commandType,
            @NotNull USER_LEVEL minUserLevel,
            int cooldownLength,
            @NotNull CooldownType cooldownType,
            String... commandPatterns
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
        Set<String> badges = messageEvent.getMessageEvent().getBadges().keySet();
        USER_LEVEL userLevel = TwitchUserLevel.getUserLevel(badges);
        if (userLevel.value < minUserLevel.value) {
            return;
        }
        
        switch (cooldownType) {
            case PER_USER -> {
                String userId = messageEvent.getUser().getId();
                Instant userInstant = recentUsages.get(userId);
                if (userInstant != null && ChronoUnit.SECONDS.between(userInstant, Instant.now()) < cooldownLength) {
                    return;
                }
            }
            case GLOBAL -> {
                if (ChronoUnit.SECONDS.between(lastUsed, Instant.now()) < cooldownLength) {
                    return;
                }
            }
        }
        
        String content = messageEvent.getMessage().toLowerCase(Locale.ENGLISH).trim();
        // remove @ from beginning of replies that doesn't appear in the Twitch UI
        if (messageEvent.getReplyInfo() != null) {
            content = content.split("\\s", 2)[1];
        }
        String command = content.split("\\s", 2)[0];
        Function<String, Boolean> patternComparison = switch (commandType) {
            case PREFIX_COMMAND -> command::equals;
            case CONTENT_COMMAND -> content::contains;
            case EXACT_MATCH_COMMAND -> exactContent::equals;
        };
    
        for (String pattern : commandPatterns) {
            if (patternComparison.apply(pattern)) {
                performCommand(pattern, userLevel, messageEvent);
                resetCooldown(messageEvent.getUser().getId());
                break;
            }
        }
    }
    
    private void resetCooldown(String userId) {
        switch (cooldownType) {
            case PER_USER -> recentUsages.put(userId, lastUsed);
            case GLOBAL -> lastUsed = Instant.now();
        }
    }
    
    private Set<String> compileCommandPattern(String[] commandWords) {
        Set<String> out = new HashSet<>();
        Collections.addAll(out, commandWords);
        return out;
    }
    
    protected abstract void performCommand(String command, USER_LEVEL userLevel, ChannelMessageEvent messageEvent);
}
