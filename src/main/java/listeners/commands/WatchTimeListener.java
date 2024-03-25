package listeners.commands;

import com.github.twitch4j.chat.events.channel.ChannelMessageEvent;
import com.github.twitch4j.common.events.domain.EventUser;
import database.DbManager;
import database.stats.WatchTimeDb;
import functions.StreamTracker;
import util.TwitchApi;
import util.TwitchUserLevel.USER_LEVEL;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.concurrent.TimeUnit;

public class WatchTimeListener extends CommandBase {
    private static final CommandType COMMAND_TYPE = CommandType.PREFIX_COMMAND;
    private static final USER_LEVEL MIN_USER_LEVEL = USER_LEVEL.DEFAULT;
    private static final int COOLDOWN = 5;
    private static final CooldownType COOLDOWN_TYPE = CooldownType.PER_USER;
    private static final String PATTERN = "!watchtime";
    
    private static final Date CUTOFF_DATE = generateCutoffDate();

    private final TwitchApi twitchApi;
    private final StreamTracker streamTracker;
    private final WatchTimeDb watchTimeDb;

    public WatchTimeListener(TwitchApi twitchApi, DbManager dbManager, StreamTracker streamTracker) {
        super(COMMAND_TYPE, MIN_USER_LEVEL, COOLDOWN, COOLDOWN_TYPE, PATTERN);
        this.twitchApi = twitchApi;
        this.streamTracker = streamTracker;
        watchTimeDb = dbManager.getWatchTimeDb();
    }

    @Override
    protected void performCommand(String command, USER_LEVEL userLevel, ChannelMessageEvent messageEvent) {
        StringBuilder output = new StringBuilder();
        int minutes = watchTimeDb.getMinutesByEventUser(messageEvent.getUser())
                      + streamTracker.getViewerMinutesById(messageEvent.getUser().getId());
        output.append(String.format(
                "@%s %s",
                messageEvent.getUser().getName(),
                getTimeString(minutes)
        ));
        if (isOldViewer(messageEvent.getUser())) {
            output.append(" since August 30, 2019");
        }
        twitchApi.channelMessage(output.toString());
    }

    private static String getTimeString(long minutes) {
        long days = TimeUnit.MINUTES.toDays(minutes);
        minutes -= TimeUnit.DAYS.toMinutes(days);
        long hours = TimeUnit.MINUTES.toHours(minutes);
        minutes -= TimeUnit.HOURS.toMinutes(hours);
        if (days > 0) {
            return String.format("%d days, %d hours, %d minutes", days, hours, minutes);
        } else if (hours > 0) {
            return String.format("%d hours, %d minutes", hours, minutes);
        } else {
            return String.format("%d minutes", minutes);
        }
    }

    //watchdata has been tracked since the cutoff date
    private boolean isOldViewer(EventUser user) {
        Date firstSeen = watchTimeDb.getFirstSeenById(user.getId());
        if (firstSeen == null) {
            return false;
        }
        return firstSeen.compareTo(CUTOFF_DATE) < 0;
    }

    //August 30, 2019
    private static Date generateCutoffDate() {
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
