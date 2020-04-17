package Listeners.Commands;

import com.gikk.twirk.enums.USER_TYPE;
import com.gikk.twirk.events.TwirkListener;
import com.gikk.twirk.types.twitchMessage.TwitchMessage;
import com.gikk.twirk.types.users.TwitchUser;

import javax.swing.*;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public abstract class CommandBase implements TwirkListener {
    
    private static final char GENERIC_COMMAND_CHAR = '!';
    
    /*
    PREFIX_COMMAND: command (first "word") matches exactly
    CONTENT_COMMAND: message contains pattern
    EXACT_MATCH_COMMAND: entire message matches pattern (case-sensitive)
    GENERIC_COMMAND: message begins with '!'
     */
    public enum CommandType {
        PREFIX_COMMAND,
        CONTENT_COMMAND,
        EXACT_MATCH_COMMAND,
        GENERIC_COMMAND
    }

    private Set<String> commandPattern;
    private CommandType commandType;
    private USER_TYPE minPrivilege;
    private int cooldownLength;
    private boolean coolingDown;

    CommandBase(CommandType commandType) {
        this.commandType = commandType;
        commandPattern = compileCommandPattern();
        minPrivilege = getMinUserPrivilege();
        cooldownLength = getCooldownLength();
        coolingDown = false;
    }

    @Override
    public void onPrivMsg(TwitchUser sender, TwitchMessage message) {
        String exactContent = message.getContent().trim();
        if(exactContent.length() == 0) {
            return;
        }
        String content = message.getContent().toLowerCase(Locale.ENGLISH).trim();
        String[] split = content.split("\\s", 2);
        String command = split[0];
        char firstChar = content.charAt(0);

        if( !coolingDown && sender.getUserType().value >= minPrivilege.value ) {
            switch (commandType) {
                case PREFIX_COMMAND:
                    for (String pattern : commandPattern) {
                        if (command.equals(pattern)) {
                            performCommand(pattern, sender, message);
                            startCooldown();
                            break;    //We don't want to fire twice for the same message
                        }
                    }
                    break;
    
                case CONTENT_COMMAND:
                    for (String pattern : commandPattern) {
                        if (content.contains(pattern)) {
                            performCommand(pattern, sender, message);
                            startCooldown();
                            break;
                        }
                    }
                    break;
    
                case EXACT_MATCH_COMMAND:
                    for (String pattern : commandPattern) {
                        if (exactContent.equals(pattern)) {
                            performCommand(pattern, sender, message);
                            startCooldown();
                            break;
                        }
                    }
                    break;
                case GENERIC_COMMAND:
                    if (firstChar == GENERIC_COMMAND_CHAR) {
                        performCommand(command, sender, message);
                    }
                    break;
            }
        }
    }

    private Set<String> compileCommandPattern() {
        String[] patterns = getCommandWords().split("\\|");
        HashSet<String> out = new HashSet<>();
        Collections.addAll(out, patterns);
        return out;
    }

    private void startCooldown() {
        if (cooldownLength == 0) {
            return;
        }
        coolingDown = true;
        Timer cooldownTimer = new Timer(cooldownLength, e -> coolingDown = false);
        cooldownTimer.setRepeats(false);
        cooldownTimer.start();
    }

    protected abstract String getCommandWords();

    protected abstract USER_TYPE getMinUserPrivilege();

    protected abstract int getCooldownLength();

    protected abstract void performCommand(String command, TwitchUser sender, TwitchMessage message);
}
