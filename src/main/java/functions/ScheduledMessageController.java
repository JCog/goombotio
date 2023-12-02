package functions;

import com.github.twitch4j.chat.events.channel.ChannelMessageEvent;
import com.github.twitch4j.chat.events.channel.RaidEvent;
import com.github.twitch4j.helix.domain.Stream;
import com.netflix.hystrix.exception.HystrixRuntimeException;
import database.DbManager;
import database.misc.SocialSchedulerDb;
import listeners.TwitchEventListener;
import util.MessageExpressionParser;
import util.TwitchApi;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static database.misc.SocialSchedulerDb.ScheduledMessage;

public class ScheduledMessageController implements TwitchEventListener {
    private static final String FOLLOW_MESSAGE_ID = "follow";
    
    private final Random random = new Random();

    private final SocialSchedulerDb socialSchedulerDb;
    private final TwitchApi twitchApi;
    private final ScheduledExecutorService scheduler;
    private final MessageExpressionParser commandParser;
    private final int intervalLength;

    private boolean running;
    private boolean activeChat;
    private boolean recentRaid;
    private String previousId = "";

    /**
     * Schedules random social media plugs on a set interval
     *
     * @param intervalLength minutes between posts
     */
    public ScheduledMessageController(
            DbManager dbManager,
            TwitchApi twitchApi,
            ScheduledExecutorService scheduler,
            MessageExpressionParser messageExpressionParser,
            int intervalLength
    ) {
        this.twitchApi = twitchApi;
        this.scheduler = scheduler;
        this.commandParser = messageExpressionParser;
        this.intervalLength = intervalLength;
        socialSchedulerDb = dbManager.getSocialSchedulerDb();
        running = false;
        activeChat = false;
        recentRaid = false;
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

    private void socialLoop() {
        if (!running || !activeChat) {
            return;
        }
        
        Stream stream;
        try {
            stream = twitchApi.getStreamByUsername(twitchApi.getStreamerUser().getLogin());
        } catch (HystrixRuntimeException e) {
            e.printStackTrace();
            System.out.println("Error retrieving stream for SocialScheduler");
            stream = null;
        }
        if (stream != null) {
            if (recentRaid) {
                ScheduledMessage message = socialSchedulerDb.getMessage(FOLLOW_MESSAGE_ID);
                if (message == null) {
                    System.out.println("Error posting scheduled follow message after a raid");
                } else {
                    twitchApi.channelAnnouncement(commandParser.parse(message.getMessage()));
                    previousId = message.getId();
                }
                recentRaid = false;
            } else {
                postRandomMsg();
            }
        }
        scheduleSocialMessages();
        activeChat = false;
    }

    private void postRandomMsg() {
        //there's definitely a more memory-efficient way to do this, but eh
        List<ScheduledMessage> choices = new ArrayList<>();
        for (ScheduledMessage message : socialSchedulerDb.getAllMessages()) {
            if (!message.getId().equals(previousId)) {
                for (int i = 0; i < message.getWeight(); i++) {
                    choices.add(message);
                }
            }
        }

        int selection = random.nextInt(choices.size());
        String message = choices.get(selection).getMessage();
        twitchApi.channelAnnouncement(commandParser.parse(message));
        previousId = choices.get(selection).getId();
    }

    private void scheduleSocialMessages() {
        LocalDateTime now = LocalDateTime.now();
        int currentMinute = now.getMinute();
        int minutesToAdd = intervalLength - (currentMinute % intervalLength);
        LocalDateTime nextInterval = now.plusMinutes(minutesToAdd);

        scheduler.schedule(this::socialLoop, now.until(nextInterval, ChronoUnit.MILLIS), TimeUnit.MILLISECONDS);
    }
    
    @Override
    public void onRaid(RaidEvent raidEvent) {
        if (raidEvent.getViewers() > 1) {
            recentRaid = true;
        }
    }
    
    @Override
    public void onChannelMessage(ChannelMessageEvent messageEvent) {
        if (activeChat) {
            return;
        }
        
        String senderId = messageEvent.getUser().getId();
        String streamerId = twitchApi.getStreamerUser().getId();
        String botId = twitchApi.getBotUser().getId();
        
        // chat is active as long as posters aren't the streamer or bot
        if (!senderId.equals(streamerId) && !senderId.equals(botId)) {
            activeChat = true;
        }
    }
}
