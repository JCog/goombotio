package Functions;

import Util.Database.SocialSchedulerDb;
import com.gikk.twirk.Twirk;
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

    private final SocialSchedulerDb socialSchedulerDb;
    private final Twirk twirk;
    private final String botName;
    private Random random;
    private boolean running;
    private boolean activeChat;
    private int previousIndex = -1;
    private int intervalLength;
    
    /**
     * Schedules random social media plugs on a set interval
     * @param twirk chat interface
     * @param intervalLength minutes between posts
     */
    public SocialScheduler(Twirk twirk, int intervalLength, String botName) {
        this.socialSchedulerDb = SocialSchedulerDb.getInstance();
        this.twirk = twirk;
        this.running = false;
        this.activeChat = false;
        this.intervalLength = intervalLength;
        this.botName = botName;
        random = new Random();
        twirk.addIrcListener(new AnyMessageListener());
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

    private void socialLoop() {
        if (running) {
            if (activeChat) {
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
