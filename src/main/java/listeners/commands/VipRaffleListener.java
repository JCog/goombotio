package listeners.commands;

import com.github.twitch4j.chat.events.channel.ChannelMessageEvent;
import com.github.twitch4j.helix.domain.User;
import database.DbManager;
import database.entries.VipRaffleItem;
import database.misc.VipRaffleDb;
import listeners.TwitchEventListener;
import util.TwitchApi;
import util.TwitchUserLevel.USER_LEVEL;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ScheduledExecutorService;

public class VipRaffleListener extends CommandBase {
    private static final CommandType COMMAND_TYPE = CommandType.PREFIX_COMMAND;
    private static final USER_LEVEL MIN_USER_LEVEL = USER_LEVEL.DEFAULT;
    private static final int COOLDOWN = 1000;
    private static final String PATTERN = "!raffle";
    
    private final TwitchApi twitchApi;
    private final VipRaffleDb vipRaffleDb;
    
    public VipRaffleListener(ScheduledExecutorService scheduler, TwitchApi twitchApi, DbManager dbManager) {
        super(scheduler, COMMAND_TYPE, MIN_USER_LEVEL, COOLDOWN, PATTERN);
        this.twitchApi = twitchApi;
        this.vipRaffleDb = dbManager.getVipRaffleDb();
    }
    
    @Override
    protected void performCommand(String command, USER_LEVEL userLevel, ChannelMessageEvent messageEvent) {
        if (userLevel == USER_LEVEL.BROADCASTER) {
            ArrayList<String> ids = performRaffle(new LinkedList<>(vipRaffleDb.getAllVipRaffleItemsCurrentMonth()));
            List<User> winners = twitchApi.getUserListByIds(ids);
            twitchApi.channelMessage(String.format(
                    "The winners of the raffle are %s, %s, %s, %s, and %s. Congrats on winning VIP for the month! jcogChamp",
                    winners.get(0).getDisplayName(),
                    winners.get(1).getDisplayName(),
                    winners.get(2).getDisplayName(),
                    winners.get(3).getDisplayName(),
                    winners.get(4).getDisplayName()
            ));
            for (User winner : winners) {
                System.out.printf("/vip %s%n", winner.getDisplayName());
            }
        } else {
            String userId = messageEvent.getMessageEvent().getUser().getId();
            String userDisplayName = TwitchEventListener.getDisplayName(messageEvent.getMessageEvent().getTags());
            ArrayList<VipRaffleItem> raffleItems = vipRaffleDb.getAllVipRaffleItemsCurrentMonth();
    
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
    
    private ArrayList<String> performRaffle(LinkedList<VipRaffleItem> raffleItems) {
        int totalEntries = 0;
        for (VipRaffleItem raffleItem : raffleItems) {
            totalEntries += raffleItem.getEntryCount();
        }
        
        Random random = new Random();
        ArrayList<String> winnerIds = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            int index = 0;
            int winningIndex = random.nextInt(totalEntries);
            for (VipRaffleItem raffleItem : raffleItems) {
                if (winningIndex <= index) {
                    winnerIds.add(raffleItem.getTwitchId());
                    raffleItems.remove(raffleItem);
                    totalEntries -= raffleItem.getEntryCount();
                    break;
                }
                index += raffleItem.getEntryCount();
            }
        }
        
        return winnerIds;
    }
}
