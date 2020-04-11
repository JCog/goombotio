package Listeners.Commands;

import Util.Database.WatchTimeDb;
import com.gikk.twirk.Twirk;
import com.gikk.twirk.enums.USER_TYPE;
import com.gikk.twirk.types.twitchMessage.TwitchMessage;
import com.gikk.twirk.types.users.TwitchUser;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.concurrent.TimeUnit;

public class WatchTimeListener extends CommandBase {
    
    private final static String PATTERN = "!watchtime";
    private final Twirk twirk;
    private final WatchTimeDb watchTimeDb;
    private final Date cutoffDate;
    
    public WatchTimeListener(Twirk twirk) {
        super(CommandType.PREFIX_COMMAND);
        this.twirk = twirk;
        watchTimeDb = WatchTimeDb.getInstance();
        cutoffDate = generateCutoffDate();
    }
    
    @Override
    protected String getCommandWords() {
        return PATTERN;
    }
    
    @Override
    protected USER_TYPE getMinUserPrivilege() {
        return USER_TYPE.DEFAULT;
    }
    
    @Override
    protected int getCooldownLength() {
        return 0;
    }
    
    @Override
    protected void performCommand(String command, TwitchUser sender, TwitchMessage message) {
        StringBuilder output = new StringBuilder();
        output.append(String.format(
                "@%s %s",
                sender.getDisplayName(),
                getTimeString(watchTimeDb.getMinutes(sender))));
        if (isOldViewer(sender)) {
            output.append(" since August 30, 2019");
        }
        twirk.channelMessage(output.toString());
    }
    
    private static String getTimeString(long minutes) {
        long days = TimeUnit.MINUTES.toDays(minutes);
        minutes -= TimeUnit.DAYS.toMinutes(days);
        long hours = TimeUnit.MINUTES.toHours(minutes);
        minutes -= TimeUnit.HOURS.toMinutes(hours);
        if (days > 0) {
            return String.format("%d days, %d hours, %d minutes", days, hours, minutes);
        }
        else if (hours > 0) {
            return String.format("%d hours, %d minutes", hours, minutes);
        }
        else {
            return String.format("%d minutes", minutes);
        }
    }
    
    //watchdata has been tracked since the cutoff date
    private boolean isOldViewer(TwitchUser user) {
        return watchTimeDb.getFirstSeen(user.getUserID()).compareTo(cutoffDate) < 0;
    }
    
    //August 30, 2019
    private Date generateCutoffDate() {
        Calendar date = new GregorianCalendar();
        date.set(Calendar.YEAR, 2019);
        date.set(Calendar.MONTH, Calendar.AUGUST);
        date.set(Calendar.DAY_OF_MONTH, 30);
        date.set(Calendar.HOUR_OF_DAY, 12);
        date.set(Calendar.MINUTE, 0);
        date.set(Calendar.SECOND, 0);
        date.set(Calendar.MILLISECOND, 0);
        return date.getTime();
    }
}
