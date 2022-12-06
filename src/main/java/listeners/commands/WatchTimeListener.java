package listeners.commands;

import com.github.twitch4j.chat.events.channel.ChannelMessageEvent;
import com.github.twitch4j.common.events.domain.EventUser;
import database.DbManager;
import database.stats.WatchTimeDb;
import functions.StreamTracker;
import util.TwitchApi;
import util.TwitchUserLevel;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class WatchTimeListener extends CommandBase {

    private static final String PATTERN = "!watchtime";
    private static final Date CUTOFF_DATE = generateCutoffDate();

    private final TwitchApi twitchApi;
    private final StreamTracker streamTracker;
    private final WatchTimeDb watchTimeDb;

    public WatchTimeListener(
            ScheduledExecutorService scheduler,
            TwitchApi twitchApi,
            DbManager dbManager,
            StreamTracker streamTracker
    ) {
        super(CommandType.PREFIX_COMMAND, scheduler);
        this.twitchApi = twitchApi;
        this.streamTracker = streamTracker;
        watchTimeDb = dbManager.getWatchTimeDb();
    }

    @Override
    public String getCommandWords() {
        return PATTERN;
    }

    @Override
    protected TwitchUserLevel.USER_LEVEL getMinUserPrivilege() {
        return TwitchUserLevel.USER_LEVEL.DEFAULT;
    }

    @Override
    protected int getCooldownLength() {
        return 0;
    }

    @Override
    protected void performCommand(String command, TwitchUserLevel.USER_LEVEL userLevel, ChannelMessageEvent messageEvent) {
        StringBuilder output = new StringBuilder();
        int minutes = watchTimeDb.getMinutesByEventUser(messageEvent.getUser())
                      + streamTracker.getViewerMinutes(messageEvent.getUser().getName());
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
        }
        else if (hours > 0) {
            return String.format("%d hours, %d minutes", hours, minutes);
        }
        else {
            return String.format("%d minutes", minutes);
        }
    }

    //watchdata has been tracked since the cutoff date
    private boolean isOldViewer(EventUser user) {
        Date firstSeen = watchTimeDb.getFirstSeenById(Long.parseLong(user.getId()));
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
