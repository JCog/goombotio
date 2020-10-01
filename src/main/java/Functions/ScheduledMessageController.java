package Functions;

import Database.Entries.ScheduledMessage;
import Database.Misc.SocialSchedulerDb;
import Util.TwirkInterface;
import Util.TwitchApi;
import com.gikk.twirk.events.TwirkListener;
import com.gikk.twirk.types.twitchMessage.TwitchMessage;
import com.gikk.twirk.types.users.TwitchUser;
import com.github.twitch4j.helix.domain.Stream;
import com.github.twitch4j.helix.domain.User;
import com.netflix.hystrix.exception.HystrixRuntimeException;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ScheduledMessageController {

    private final SocialSchedulerDb socialSchedulerDb = SocialSchedulerDb.getInstance();
    private final AnyMessageListener anyMessageListener = new AnyMessageListener();
    private final Random random = new Random();
    
    private final TwirkInterface twirk;
    private final TwitchApi twitchApi;
    private final ScheduledExecutorService scheduler;
    private final User botUser;
    private final int intervalLength;
    
    private boolean running;
    private boolean activeChat;
    private String previousId = "";
    
    /**
     * Schedules random social media plugs on a set interval
     * @param twirk chat interface
     * @param intervalLength minutes between posts
     */
    public ScheduledMessageController(
            TwirkInterface twirk,
            TwitchApi twitchApi,
            ScheduledExecutorService scheduler,
            User botUser,
            int intervalLength
    ) {
        this.twirk = twirk;
        this.twitchApi = twitchApi;
        this.scheduler = scheduler;
        this.botUser = botUser;
        this.intervalLength = intervalLength;
        running = false;
        activeChat = false;
    }
    
    /**
     * Starts SocialScheduler. One random message every interval, and only if a chat message has been posted in the
     * current interval to prevent bot spam with an inactive chat
     */
    public void start() {
        running = true;
        scheduleSocialMessages();
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
            Stream stream;
            try {
                stream = twitchApi.getStream();
            }
            catch (HystrixRuntimeException e) {
                e.printStackTrace();
                System.out.println("Error retrieving stream for SocialScheduler");
                return;
            }
            if (activeChat && stream != null) {
                postRandomMsg();
            }
            scheduleSocialMessages();
            activeChat = false;
        }
    }
    
    private void postRandomMsg() {
        //there's definitely a more memory-efficient way to do this, but eh
        ArrayList<ScheduledMessage> choices = new ArrayList<>();
        for (ScheduledMessage message : socialSchedulerDb.getAllMessages()) {
            if (!message.getId().equals(previousId)) {
                for (int i = 0; i < message.getWeight(); i++) {
                    choices.add(message);
                }
            }
        }
        
        int selection = random.nextInt(choices.size());
        twirk.channelMessage(choices.get(selection).getMessage());
        previousId = choices.get(selection).getId();
    }

    private void scheduleSocialMessages() {
        LocalDateTime now = LocalDateTime.now();
        int currentMinute = now.getMinute();
        int minutesToAdd = intervalLength - (currentMinute % intervalLength);
        LocalDateTime nextInterval = now.plusMinutes(minutesToAdd);

        scheduler.schedule(this::socialLoop, now.until(nextInterval, ChronoUnit.MILLIS), TimeUnit.MILLISECONDS);
    }

    private class AnyMessageListener implements TwirkListener {
        @Override
        public void onPrivMsg(TwitchUser sender, TwitchMessage message) {
            if (!sender.isOwner() && !sender.getUserName().toLowerCase().equals(botUser.getLogin())) {
                activeChat = true;
            }
        }
    }
}
