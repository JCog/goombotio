package Functions;

import Database.Misc.SocialSchedulerDb;
import Util.Settings;
import Util.TwirkInterface;
import com.gikk.twirk.events.TwirkListener;
import com.gikk.twirk.types.twitchMessage.TwitchMessage;
import com.gikk.twirk.types.users.TwitchUser;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SocialScheduler {

    private final SocialSchedulerDb socialSchedulerDb = SocialSchedulerDb.getInstance();
    private final AnyMessageListener anyMessageListener = new AnyMessageListener();
    private final String botName = Settings.getTwitchUsername();
    private final Random random = new Random();
    
    private final TwirkInterface twirk;
    private final StreamInfo streamInfo;
    private final int intervalLength;
    
    private boolean running;
    private boolean activeChat;
    private int previousIndex = -1;
    
    /**
     * Schedules random social media plugs on a set interval
     * @param twirk chat interface
     * @param intervalLength minutes between posts
     */
    public SocialScheduler(TwirkInterface twirk, StreamInfo streamInfo, int intervalLength) {
        this.twirk = twirk;
        this.streamInfo = streamInfo;
        this.running = false;
        this.activeChat = false;
        this.intervalLength = intervalLength;
    }
    
    /**
     * Starts SocialScheduler. One random message every interval, and only if a chat message has been posted in the
     * current interval to prevent bot spam with an inactive chat
     */
    public void start() {
        running = true;
        scheduleSocialMsgs();
    }
    
    /**
     * Stops SocialScheduler
     */
    public void stop() {
        running = false;
    }
    
    public AnyMessageListener getListener() {
        return anyMessageListener;
    }

    private void socialLoop() {
        if (running) {
            if (activeChat && streamInfo.isLive()) {
                postRandomMsg();
            }
            scheduleSocialMsgs();
            activeChat = false;
        }
    }
    
    private void postRandomMsg() {
        ArrayList<String > messages = socialSchedulerDb.getAllMessages();
        
        int index = random.nextInt(messages.size());
        while(index == previousIndex) {
            index = random.nextInt(messages.size());
        }
        
        twirk.channelMessage(messages.get(index));
        previousIndex = index;
    }

    private void scheduleSocialMsgs() {
        LocalDateTime now = LocalDateTime.now();
        int currentMinute = now.getMinute();
        int minutesToAdd = intervalLength - (currentMinute % intervalLength);
        LocalDateTime nextInterval = now.plusMinutes(minutesToAdd);

        ScheduledExecutorService ses = Executors.newScheduledThreadPool(1);
        ses.schedule(this::socialLoop, now.until(nextInterval, ChronoUnit.MILLIS), TimeUnit.MILLISECONDS);
    }

    private class AnyMessageListener implements TwirkListener {
        @Override
        public void onPrivMsg(TwitchUser sender, TwitchMessage message) {
            if (!sender.isOwner() && !sender.getUserName().toLowerCase().equals(botName)) {
                activeChat = true;
            }
        }
    }
}
