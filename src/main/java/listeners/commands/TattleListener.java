package listeners.commands;

import com.github.twitch4j.chat.events.channel.ChannelMessageEvent;
import com.github.twitch4j.helix.domain.User;
import database.misc.TattleDb;
import database.misc.TattleDb.TattleItem;
import util.CommonUtils;
import util.TwitchApi;
import util.TwitchUserLevel.USER_LEVEL;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class TattleListener extends CommandBase {
    private static final CommandType COMMAND_TYPE = CommandType.PREFIX_COMMAND;
    private static final USER_LEVEL MIN_USER_LEVEL = USER_LEVEL.DEFAULT;
    private static final int COOLDOWN = 5;
    private static final CooldownType COOLDOWN_TYPE = CooldownType.COMBINED;
    private final static String PATTERN_TATTLE = "!tattle";
    private final static String PATTERN_ADD = "!addtattle";
    
    private final TattleDb tattleDb;
    private final TwitchApi twitchApi;
    private final Random random = new Random();

    public TattleListener(CommonUtils commonUtils) {
        super(COMMAND_TYPE, MIN_USER_LEVEL, COOLDOWN, COOLDOWN_TYPE, PATTERN_TATTLE, PATTERN_ADD);
        tattleDb = commonUtils.getDbManager().getTattleDb();
        twitchApi = commonUtils.getTwitchApi();
    }

    @Override
    protected void performCommand(String command, USER_LEVEL userLevel, ChannelMessageEvent messageEvent) {
        String trimmedMessage = messageEvent.getMessage().trim();
        String[] messageSplit = trimmedMessage.split(" ");
        switch (command) {
            case PATTERN_TATTLE: {
                if (messageSplit.length == 1) {
                    twitchApi.channelMessage(tattleItemToString(tattleDb.getRandomTattle()));
                    return;
                }

                String username = messageSplit[1].toLowerCase();
                
                User user = twitchApi.getUserByUsername(username);
                if (user != null) {
                    TattleItem tattle = tattleDb.getTattle(user.getId());
                    if (tattle != null) {
                        twitchApi.channelMessage(tattleItemToString(tattle));
                        return;
                    }
                }
    
                List<TattleItem> allTattles = tattleDb.getAllTattles();
                List<User> users = twitchApi.getUserListByIds(allTattles.stream().map(TattleItem::getTwitchId).collect(Collectors.toList()));
                List<User> userOptions = new ArrayList<>();
    
                for (User tattleUser : users) {
                    if (tattleUser.getLogin().contains(username)) {
                        userOptions.add(tattleUser);
                    }
                }
    
                if (userOptions.isEmpty()) {
                    twitchApi.channelMessage(String.format("No matches for \"%s\"", username));
                    return;
                }
                
                int index = random.nextInt(userOptions.size());
                TattleItem outputTattle = allTattles.stream()
                        .filter(tattleItem -> userOptions.get(index).getId().equals(tattleItem.getTwitchId()))
                        .findAny()
                        .orElse(null);
                if (outputTattle == null) {
                    //sanity check
                    twitchApi.channelMessage(String.format("No matches for \"%s\"", username));
                    return;
                }
    
                twitchApi.channelMessage(tattleItemToString(outputTattle));
                break;
            }
            case PATTERN_ADD: {
                if (userLevel != USER_LEVEL.BROADCASTER) {
                    return;
                }
                
                if (messageSplit.length < 3) {
                    twitchApi.channelMessage("ERROR: not enough arguments");
                    return;
                }

                User user = twitchApi.getUserByUsername(messageSplit[1]);
                if (user == null) {
                    twitchApi.channelMessage(String.format("ERROR: unknown user \"%s\"", messageSplit[1]));
                    return;
                }

                int start = trimmedMessage.indexOf('"');
                int end = trimmedMessage.lastIndexOf('"');
                if (start != end) { //valid quotes
                    tattleDb.addTattle(user.getId(), trimmedMessage.substring(start + 1, end));
                    twitchApi.channelMessage(String.format("Added tattle for %s", user.getDisplayName()));
                } else if (start == -1) { //no quotes
                    twitchApi.channelMessage("ERROR: no quotation marks");
                } else { //one quote mark
                    twitchApi.channelMessage("ERROR: not enough quotation marks");
                }
            }
            break;
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
