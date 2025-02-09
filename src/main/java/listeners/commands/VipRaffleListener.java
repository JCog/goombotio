package listeners.commands;

import com.github.twitch4j.chat.events.channel.ChannelMessageEvent;
import com.github.twitch4j.helix.domain.*;
import com.netflix.hystrix.exception.HystrixRuntimeException;
import database.misc.VipDb;
import database.misc.VipRaffleDb;
import listeners.TwitchEventListener;
import util.CommonUtils;
import util.TwitchApi;
import util.TwitchUserLevel.USER_LEVEL;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import static database.misc.VipRaffleDb.VipRaffleItem;

public class VipRaffleListener extends CommandBase {
    private static final CommandType COMMAND_TYPE = CommandType.PREFIX_COMMAND;
    private static final USER_LEVEL MIN_USER_LEVEL = USER_LEVEL.DEFAULT;
    private static final int COOLDOWN = 5;
    private static final CooldownType COOLDOWN_TYPE = CooldownType.PER_USER;
    private static final int WINNER_COUNT = 5;
    private static final String PATTERN = "!raffle";
    
    private final TwitchApi twitchApi;
    private final VipRaffleDb vipRaffleDb;
    private final VipDb vipDb;
    
    public VipRaffleListener(CommonUtils commonUtils) {
        super(COMMAND_TYPE, MIN_USER_LEVEL, COOLDOWN, COOLDOWN_TYPE, PATTERN);
        twitchApi = commonUtils.twitchApi();
        vipRaffleDb = commonUtils.dbManager().getVipRaffleDb();
        vipDb = commonUtils.dbManager().getVipDb();
    }
    
    private record UserRecord(String id, String username) {}
    
    @Override
    protected void performCommand(String command, USER_LEVEL userLevel, ChannelMessageEvent messageEvent) {
        if (userLevel == USER_LEVEL.BROADCASTER) {
            twitchApi.channelMessage("Performing VIP raffle...");
            
            List<VipRaffleItem> vipRaffleItems = new LinkedList<>(vipRaffleDb.getAllVipRaffleItemsPrevMonth());
            for (int i = 0; i < vipRaffleItems.size(); i++) {
                System.out.printf(
                        "%d. %s (%d)%n",
                        i + 1,
                        vipRaffleItems.get(i).displayName(),
                        vipRaffleItems.get(i).entryCount()
                );
            }
            
            List<String> newWinnerIds;
            List<User> winners;
            try {
                newWinnerIds = performRaffle(vipRaffleItems);
                winners = twitchApi.getUserListByIds(newWinnerIds); // usernames may have changed
            } catch (HystrixRuntimeException e) {
                twitchApi.channelMessage("Twitch API error, please try again.");
                return;
            }
            
            if (winners.isEmpty()) {
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
            
            
            // don't touch repeat winners
            List<String> oldWinnerIds = vipDb.getAllRaffleWinnerUserIds();
            List<String> repeatWinners = new ArrayList<>();
            for (String userId : newWinnerIds) {
                if (oldWinnerIds.contains(userId)) {
                    repeatWinners.add(userId);
                }
            }
            oldWinnerIds.removeAll(repeatWinners);
            newWinnerIds.removeAll(repeatWinners);
            
            // retrieve usernames for logging purposes
            List<UserRecord> oldWinners = new ArrayList<>();
            List<UserRecord> newWinners = new ArrayList<>();
            try {
                List<User> oldWinnerUsers = twitchApi.getUserListByIds(oldWinnerIds);
                List<User> newWinnerUsers = twitchApi.getUserListByIds(newWinnerIds);
                for (User user : oldWinnerUsers) {
                    oldWinners.add(new UserRecord(user.getId(), user.getDisplayName()));
                }
                for (User user : newWinnerUsers) {
                    newWinners.add(new UserRecord(user.getId(), user.getDisplayName()));
                }
            } catch (HystrixRuntimeException e) {
                System.out.println("Twitch API error getting old/new winner usernames.");
                for (String id : oldWinnerIds) {
                    oldWinners.add(new UserRecord(id, null));
                }
                for (String id : newWinnerIds) {
                    newWinners.add(new UserRecord(id, null));
                }
            }
            
            List<String> currentVipIds;
            try {
                currentVipIds = twitchApi.getChannelVips().stream()
                        .map(ChannelVip::getUserId)
                        .toList();
            } catch (HystrixRuntimeException e) {
                twitchApi.channelMessage("Twitch API error getting current VIPs. Unable to automatically update.");
                return;
            }
            
            // remove old VIPs
            boolean addRemoveError = false; // don't want to overload chat in case of multiple API errors
            for (UserRecord oldWinner : oldWinners) {
                vipDb.editRaffleWinnerProp(oldWinner.id(), false);
                
                if (!vipDb.hasVip(oldWinner.id()) && currentVipIds.contains(oldWinner.id())) {
                    try {
                        twitchApi.vipRemove(oldWinner.id());
                        System.out.printf(
                                "VIP removed from %s (%s)%n",
                                oldWinner.username() == null ? "?" : oldWinner.username(),
                                oldWinner.id()
                        );
                    } catch (HystrixRuntimeException e) {
                        if (!addRemoveError) {
                            twitchApi.channelMessage("Error(s) adding/remove VIPs. Please check logs.");
                            addRemoveError = true;
                        }
                        System.out.printf(
                                "Error removing VIP from %s (%s)%n",
                                oldWinner.username() == null ? "?" : oldWinner.username(),
                                oldWinner.id()
                        );
                    }
                }
            }
            
            // add new VIPs
            for (UserRecord newWinner : newWinners) {
                vipDb.editRaffleWinnerProp(newWinner.id(), true);
                
                if (!currentVipIds.contains(newWinner.id())) {
                    try {
                        twitchApi.vipAdd(newWinner.id());
                        System.out.printf("VIP added to %s (%s)%n",
                                newWinner.username() == null ? "?" : newWinner.username(),
                                newWinner.id()
                        );
                    } catch (HystrixRuntimeException e) {
                        if (!addRemoveError) {
                            twitchApi.channelMessage("Error(s) adding/remove VIPs. Please check logs.");
                            addRemoveError = true;
                        }
                        System.out.printf("Error adding VIP to %s (%s)%n",
                                newWinner.username() == null ? "?" : newWinner.username(),
                                newWinner.id()
                        );
                    }
                }
            }
        } else {
            String userId = messageEvent.getMessageEvent().getUser().getId();
            String userDisplayName = TwitchEventListener.getDisplayName(messageEvent.getMessageEvent());
            VipRaffleItem userRaffleItem = vipRaffleDb.getVipRaffleItem(userId);
            int userEntryCount = userRaffleItem == null ? 0 : userRaffleItem.entryCount();
            
            twitchApi.channelMessage(String.format(
                    "@%s You have %d raffle entr%s for this month%s.",
                    userDisplayName,
                    userEntryCount,
                    userEntryCount == 1 ? "y" : "ies",
                    userEntryCount == 0 ? "" : String.format(
                            " (~%.1f%% of all entries)",
                            (float) userEntryCount * 100 / vipRaffleDb.getTotalEntryCountCurrentMonth()
                    )
            ));
        }
        
        
    }
    
    public List<String> performRaffle(List<VipRaffleItem> raffleItems) throws HystrixRuntimeException {
        List<String> filteredIds = raffleItems.stream()
                .map(VipRaffleItem::twitchId)
                .collect(Collectors.toCollection(ArrayList::new));
        List<String> bannedUserIds = twitchApi.getBannedUsers(filteredIds).stream()
                .map(BannedUser::getUserId)
                .toList();
        List<String> modListIds = twitchApi.getMods(twitchApi.getStreamerUser().getId()).stream()
                .map(Moderator::getUserId)
                .toList();
        List<String> blacklistIds = vipDb.getAllBlacklistedUserIds();
        
        filteredIds.removeAll(bannedUserIds);
        filteredIds.removeAll(modListIds);
        filteredIds.removeAll(blacklistIds);
        
        
        Random random = new Random();
        List<String> winnerIds = new ArrayList<>();
        
        double totalWeight = 0;
        for (VipRaffleItem item : raffleItems) {
            totalWeight += item.entryCount();
        }
        
        while (winnerIds.size() < WINNER_COUNT && !raffleItems.isEmpty()) {
            double indexWinner = random.nextDouble() * totalWeight;
            double indexCurrent = 0;
            VipRaffleItem winner = null;
            for (VipRaffleItem item : raffleItems) {
                indexCurrent += item.entryCount();
                if (indexCurrent >= indexWinner) {
                    winner = item;
                    break;
                }
            }
            if (winner == null) {
                // should never happen
                continue;
            }
            
            totalWeight -= winner.entryCount();
            raffleItems.remove(winner);
    
            // skip if banned user, mod, or blacklisted
            if (!filteredIds.contains(winner.twitchId())) {
                continue;
            }
            
            // skip users that don't follow the channel
            InboundFollow channelFollower = twitchApi.getChannelFollower(twitchApi.getStreamerUser().getId(), winner.twitchId());
            if (channelFollower == null) {
                continue;
            }
            
            winnerIds.add(winner.twitchId());
        }
        
        return winnerIds;
    }
}
