package listeners.commands;

import com.gikk.twirk.types.twitchMessage.TwitchMessage;
import com.gikk.twirk.types.users.TwitchUser;
import com.github.twitch4j.helix.domain.User;
import database.DbManager;
import database.entries.TattleItem;
import database.misc.TattleDb;
import util.TwirkInterface;
import util.TwitchApi;
import util.TwitchUserLevel;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Collectors;

public class TattleListener extends CommandBase {

    private final static String PATTERN_TATTLE = "!tattle";
    private final static String PATTERN_ADD = "!addtattle";
    private final TattleDb tattleDb;
    private final TwirkInterface twirk;
    private final TwitchApi twitchApi;
    private final Random random = new Random();

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

                String username = messageSplit[1].toLowerCase();
                
                User user = twitchApi.getUserByUsername(username);
                if (user != null) {
                    TattleItem tattle = tattleDb.getTattle(user.getId());
                    if (tattle != null) {
                        twirk.channelMessage(tattleItemToString(tattle));
                        return;
                    }
                }
    
                ArrayList<TattleItem> allTattles = tattleDb.getAllTattles();
                List<User> users = twitchApi.getUserListByIds(allTattles.stream().map(TattleItem::getTwitchId).collect(Collectors.toList()));
                ArrayList<User> userOptions = new ArrayList<>();
    
                for (User tattleUser : users) {
                    if (tattleUser.getLogin().contains(username)) {
                        userOptions.add(tattleUser);
                    }
                }
    
                if (userOptions.isEmpty()) {
                    twirk.channelMessage(String.format("No matches for \"%s\"", username));
                    return;
                }
                
                int index = random.nextInt(userOptions.size());
                TattleItem outputTattle = allTattles.stream()
                        .filter(tattleItem -> userOptions.get(index).getId().equals(tattleItem.getTwitchId()))
                        .findAny()
                        .orElse(null);
                if (outputTattle == null) {
                    //sanity check
                    twirk.channelMessage(String.format("No matches for \"%s\"", username));
                    return;
                }
                
                twirk.channelMessage(tattleItemToString(outputTattle));
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
        User user = twitchApi.getUserById(tattleItem.getTwitchId());
        if (user == null) {
            return String.format("ERROR: unknown user ID \"%s\"", tattleItem.getTwitchId());
        }
        String username = user.getDisplayName();
        return String.format("%s: %s", username, tattleItem.getTattle());
    }
}
