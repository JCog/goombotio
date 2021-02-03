package listeners.commands;

import com.gikk.twirk.types.twitchMessage.TwitchMessage;
import com.gikk.twirk.types.users.TwitchUser;
import com.github.twitch4j.helix.domain.User;
import com.jcog.utils.TwitchApi;
import com.jcog.utils.TwitchUserLevel;
import com.jcog.utils.database.DbManager;
import com.jcog.utils.database.entries.TattleItem;
import com.jcog.utils.database.misc.TattleDb;
import util.TwirkInterface;

import java.util.concurrent.ScheduledExecutorService;

public class TattleListener extends CommandBase {

    private final static String PATTERN_TATTLE = "!tattle";
    private final static String PATTERN_ADD = "!addtattle";
    private final TattleDb tattleDb;
    private final TwirkInterface twirk;
    private final TwitchApi twitchApi;

    public TattleListener(ScheduledExecutorService scheduler,
                          DbManager dbManager,
                          TwirkInterface twirk,
                          TwitchApi twitchApi) {
        super(CommandType.PREFIX_COMMAND, scheduler);
        this.tattleDb = dbManager.getTattleDb();
        this.twirk = twirk;
        this.twitchApi = twitchApi;
    }

    @Override
    public String getCommandWords() {
        return String.join("|", PATTERN_TATTLE, PATTERN_ADD);
    }

    @Override
    protected TwitchUserLevel.USER_LEVEL getMinUserPrivilege() {
        return TwitchUserLevel.USER_LEVEL.DEFAULT;
    }

    @Override
    protected int getCooldownLength() {
        return 5 * 1000;
    }

    @Override
    protected void performCommand(String command, TwitchUser sender, TwitchMessage message) {
        String trimmedMessage = message.getContent().trim();
        String[] messageSplit = trimmedMessage.split(" ");
        switch (command) {
            case PATTERN_TATTLE: {
                if (messageSplit.length == 1) {
                    twirk.channelMessage(tattleItemToString(tattleDb.getRandomTattle()));
                    return;
                }

                User user = twitchApi.getUserByUsername(messageSplit[1]);
                if (user == null) {
                    twirk.channelMessage(String.format("Unknown user \"%s\"", messageSplit[1]));
                    return;
                }

                TattleItem tattle = tattleDb.getTattle(user.getId());
                if (tattle == null) {
                    twirk.channelMessage(String.format("No tattle data for %s", user.getDisplayName()));
                    return;
                }
                twirk.channelMessage(tattleItemToString(tattle));
                break;
            }
            case PATTERN_ADD: {
                if (sender.isOwner()) {
                    if (messageSplit.length < 3) {
                        twirk.channelMessage("ERROR: not enough arguments");
                        return;
                    }

                    User user = twitchApi.getUserByUsername(messageSplit[1]);
                    if (user == null) {
                        twirk.channelMessage(String.format("ERROR: unknown user \"%s\"", messageSplit[1]));
                        return;
                    }

                    int start = trimmedMessage.indexOf('"');
                    int end = trimmedMessage.lastIndexOf('"');
                    if (start != end) { //valid quotes
                        tattleDb.addTattle(user.getId(), trimmedMessage.substring(start + 1, end));
                        twirk.channelMessage(String.format("Added tattle for %s", user.getDisplayName()));
                    }
                    else if (start == -1) { //no quotes
                        twirk.channelMessage("ERROR: no quotation marks");
                    }
                    else { //one quote mark
                        twirk.channelMessage("ERROR: not enough quotation marks");
                    }
                }
                break;
            }
        }
    }

    private String tattleItemToString(TattleItem tattleItem) {
        String username = twitchApi.getUserById(tattleItem.getTwitchId()).getDisplayName();
        return String.format("%s: %s", username, tattleItem.getTattle());
    }
}
