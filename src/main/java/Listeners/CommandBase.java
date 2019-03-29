package Listeners;

import com.gikk.twirk.enums.USER_TYPE;
import com.gikk.twirk.events.TwirkListener;
import com.gikk.twirk.types.twitchMessage.TwitchMessage;
import com.gikk.twirk.types.users.TwitchUser;

import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public abstract class CommandBase implements TwirkListener {
    public enum CommandType {
        PREFIX_COMMAND, CONTENT_COMMAND, ALL_COMMANDS
    }

    private Set<String> commandPattern;
    private CommandType commandType;
    private USER_TYPE minPrivilege;

    protected CommandBase(CommandType commandType) {
        this.commandType = commandType;
        commandPattern = complileCommandPattern();
        minPrivilege = getMinUserPrivilege();
    }

    @Override
    public void onPrivMsg(TwitchUser sender, TwitchMessage message) {
        String content = message.getContent().toLowerCase(Locale.ENGLISH).trim();
        String[] split = content.split("\\s");
        String command = split[0];
        HashSet<String> allWords = new HashSet<>();
        Collections.addAll(allWords, split);

        if(sender.getUserType().value >= minPrivilege.value) {
            switch (commandType) {
                case PREFIX_COMMAND:
                    for (String pattern : commandPattern) {
                        if (command.startsWith(pattern)) {
                            performCommand(pattern, sender, message);
                        }
                    }
                    break;

                case CONTENT_COMMAND:
                default:
                    for (String pattern : commandPattern) {
                        if (content.contains(pattern)) {
                            performCommand(pattern, sender, message);
                            break;
                        }
                    }
                    break;
                case ALL_COMMANDS:
                    if (allWords.containsAll(commandPattern)) {
                        performCommand(content, sender, message);
                    }
            }
        }
    }

    private Set<String> complileCommandPattern() {
        String[] patterns = getCommandWords().toLowerCase(Locale.ENGLISH).split("\\|");
        HashSet<String> out = new HashSet<>();
        Collections.addAll(out, patterns);
        return out;
    }

    protected  abstract String getCommandWords();

    protected abstract USER_TYPE getMinUserPrivilege();

    protected abstract void performCommand(String command, TwitchUser sender, TwitchMessage message);
}
