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
import java.util.TimerTask;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static database.misc.SocialSchedulerDb.ScheduledMessage;

public class ScheduledMessageController implements TwitchEventListener {
    private static final long INTERVAL_LENGTH = 1; //minutes
    private static final String FOLLOW_MESSAGE_ID = "follow";
    
    private final Random random = new Random();

    private final SocialSchedulerDb socialSchedulerDb;
    private final TwitchApi twitchApi;
    private final MessageExpressionParser commandParser;

    private boolean activeChat;
    private boolean recentRaid;
    private String previousId = "";
    
    public ScheduledMessageController(
            DbManager dbManager,
            TwitchApi twitchApi,
            ScheduledExecutorService scheduler,
            MessageExpressionParser messageExpressionParser
    ) {
        this.twitchApi = twitchApi;
        this.commandParser = messageExpressionParser;
        socialSchedulerDb = dbManager.getSocialSchedulerDb();
        activeChat = false;
        recentRaid = false;
        
        initScheduledMessages(scheduler);
    }
    
    private void initScheduledMessages(ScheduledExecutorService scheduler) {
        long intervalLengthMillis = INTERVAL_LENGTH * 60 * 1000;
        LocalDateTime nowMinutes = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);
        long minutesToAdd = INTERVAL_LENGTH - (nowMinutes.getMinute() % INTERVAL_LENGTH);
        LocalDateTime nextInterval = nowMinutes.plusMinutes(minutesToAdd);
        System.out.println("nowMinutes:   " + nowMinutes);
        System.out.println("nextInterval: " + nextInterval);
        
        scheduler.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                // only post if chat is active
//                if (!activeChat) {
//                    return;
//                }
                activeChat = false;
                
                // only post if stream is live
                Stream stream;
                try {
                    stream = twitchApi.getStreamByUserId(twitchApi.getStreamerUser().getId());
                    System.out.println(stream);
                } catch (HystrixRuntimeException e) {
                    e.printStackTrace();
                    System.out.println("Error retrieving stream for SocialScheduler");
                    stream = null;
                }
//                if (stream == null) {
//                    return;
//                }
                
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
        }, LocalDateTime.now().until(nextInterval, ChronoUnit.MILLIS), intervalLengthMillis, TimeUnit.MILLISECONDS);
    }

    private void postRandomMsg() {
        System.out.println("random message");
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
