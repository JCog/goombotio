package listeners.commands;

import com.github.twitch4j.chat.events.channel.ChannelMessageEvent;
import com.github.twitch4j.helix.domain.BannedUser;
import com.github.twitch4j.helix.domain.InboundFollow;
import com.github.twitch4j.helix.domain.Moderator;
import com.github.twitch4j.helix.domain.User;
import com.netflix.hystrix.exception.HystrixRuntimeException;
import database.DbManager;
import database.misc.VipDb;
import database.misc.VipRaffleDb;
import util.TwitchApi;
import util.TwitchUserLevel.USER_LEVEL;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Collectors;

import static database.misc.VipRaffleDb.VipRaffleItem;

public class VipRaffleListener extends CommandBase {
    private static final CommandType COMMAND_TYPE = CommandType.PREFIX_COMMAND;
    private static final USER_LEVEL MIN_USER_LEVEL = USER_LEVEL.DEFAULT;
    private static final int COOLDOWN = 1000;
    private static final int WINNER_COUNT = 5;
    private static final String PATTERN = "!raffle";
    
    private final TwitchApi twitchApi;
    private final VipRaffleDb vipRaffleDb;
    private final VipDb vipDb;
    
    public VipRaffleListener(ScheduledExecutorService scheduler, TwitchApi twitchApi, DbManager dbManager) {
        super(scheduler, COMMAND_TYPE, MIN_USER_LEVEL, COOLDOWN, PATTERN);
        this.twitchApi = twitchApi;
        this.vipRaffleDb = dbManager.getVipRaffleDb();
        this.vipDb = dbManager.getVipDb();
    }
    
    @Override
    protected void performCommand(String command, USER_LEVEL userLevel, ChannelMessageEvent messageEvent) {
        if (userLevel == USER_LEVEL.BROADCASTER) {
            twitchApi.channelMessage("Performing VIP raffle...");
            
            List<VipRaffleItem> vipRaffleItems = new LinkedList<>(vipRaffleDb.getAllVipRaffleItemsPrevMonth());
            for (int i = 0; i < vipRaffleItems.size(); i++) {
                System.out.printf(
                        "%d. %s (%d)%n",
                        i + 1,
                        vipRaffleItems.get(i).getDisplayName(),
                        vipRaffleItems.get(i).getEntryCount()
                );
            }
            
            List<String> ids;
            List<User> winners;
            try {
                ids = performRaffle(vipRaffleItems);
                winners = twitchApi.getUserListByIds(ids); // usernames may have changed
            } catch (HystrixRuntimeException e) {
                twitchApi.channelMessage("Twitch API error, please try again.");
                return;
            }
            
            if (winners.size() == 0) {
                twitchApi.channelMessage("There are no raffle entries, so nobody wins.");
            } else if (winners.size() == 1) {
                twitchApi.channelMessage(String.format(
                        "The winner of the raffle is %s. Congrats on winning VIP for the month! jcogChamp",
                        winners.get(0).getDisplayName()
                ));
            } else if (winners.size() == 2) {
                twitchApi.channelMessage(String.format(
                        "The winners of the raffle are %s and %s. Congrats on winning VIP for the month! jcogChamp",
                        winners.get(0).getDisplayName(),
                        winners.get(1).getDisplayName()
                ));
            } else {
                StringBuilder output = new StringBuilder();
                output.append("The winners of the raffle are ");
                for (int i = 0; i < winners.size() - 1; i++) {
                    output.append(String.format("%s, ", winners.get(i).getDisplayName()));
                }
                output.append(String.format(
                        "and %s. Congrats on winning VIP for the month! jcogChamp",
                        winners.get(winners.size() - 1).getDisplayName()
                ));
                twitchApi.channelMessage(output.toString());
            }
            
            for (User winner : winners) {
                System.out.printf("/vip %s%n", winner.getDisplayName());
            }
        } else {
            String userId = messageEvent.getMessageEvent().getUser().getId();
            String userDisplayName = messageEvent.getMessageEvent().getUserName();
            if (messageEvent.getMessageEvent().getUserDisplayName().isPresent()) {
                userDisplayName = messageEvent.getMessageEvent().getUserDisplayName().get();
            }
            List<VipRaffleItem> raffleItems = vipRaffleDb.getAllVipRaffleItemsCurrentMonth();
    
            VipRaffleItem userRaffleItem = vipRaffleDb.getVipRaffleItem(userId);
            int userEntryCount = userRaffleItem == null ? 0 : userRaffleItem.getEntryCount();
            int totalEntryCount = 0;
            for (VipRaffleItem raffleItem : raffleItems) {
                totalEntryCount += raffleItem.getEntryCount();
            }
            
            twitchApi.channelMessage(String.format(
                    "@%s You have %d raffle entr%s for this month%s.",
                    userDisplayName,
                    userEntryCount,
                    userEntryCount == 1 ? "y" : "ies",
                    userEntryCount == 0 ? "" : String.format(
                            " (~%.1f%% of all entries)",
                            (float) userEntryCount * 100 / totalEntryCount
                    )
            ));
        }
        
        
    }
    
    private List<String> performRaffle(List<VipRaffleItem> raffleItems) throws HystrixRuntimeException {
        List<String> filteredIds = raffleItems.stream()
                .map(VipRaffleItem::getTwitchId)
                .collect(Collectors.toList());
        List<String> bannedUserIds = twitchApi.getBannedUsers(filteredIds).stream()
                .map(BannedUser::getUserId)
                .collect(Collectors.toList());
        List<String> modListIds = twitchApi.getMods(twitchApi.getStreamerUser().getId()).stream()
                .map(Moderator::getUserId)
                .collect(Collectors.toList());
        List<String> blacklistIds = vipDb.getAllBlacklistedUserIds();
        
        filteredIds.removeAll(bannedUserIds);
        filteredIds.removeAll(modListIds);
        filteredIds.removeAll(blacklistIds);
        
        
        Random random = new Random();
        List<String> winnerIds = new ArrayList<>();
        
        double totalWeight = 0;
        for (VipRaffleItem item : raffleItems) {
            totalWeight += item.getEntryCount();
        }
        
        while (winnerIds.size() < WINNER_COUNT && raffleItems.size() > 0) {
            double indexWinner = random.nextDouble() * totalWeight;
            double indexCurrent = 0;
            VipRaffleItem winner = null;
            for (VipRaffleItem item : raffleItems) {
                indexCurrent += item.getEntryCount();
                if (indexCurrent >= indexWinner) {
                    winner = item;
                    break;
                }
            }
            if (winner == null) {
                // should never happen
                continue;
            }
            
            totalWeight -= winner.getEntryCount();
            raffleItems.remove(winner);
    
            // skip if banned user, mod, or blacklisted
            if (!filteredIds.contains(winner.getTwitchId())) {
                continue;
            }
            
            // skip users that don't follow the channel
            InboundFollow channelFollower = twitchApi.getChannelFollower(twitchApi.getStreamerUser().getId(), winner.getTwitchId());
            if (channelFollower == null) {
                continue;
            }
            
            winnerIds.add(winner.getTwitchId());
        }
        
        return winnerIds;
    }
}
