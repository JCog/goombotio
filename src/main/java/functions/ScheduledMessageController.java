package functions;

import com.github.twitch4j.chat.events.channel.ChannelMessageEvent;
import com.github.twitch4j.eventsub.events.ChannelRaidEvent;
import com.github.twitch4j.helix.domain.Stream;
import com.netflix.hystrix.exception.HystrixRuntimeException;
import database.misc.SocialSchedulerDb;
import listeners.TwitchEventListener;
import util.CommonUtils;
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
    private static final long INTERVAL_LENGTH = 20; //minutes
    private static final String FOLLOW_MESSAGE_ID = "follow";
    
    private final Random random = new Random();

    private final SocialSchedulerDb socialSchedulerDb;
    private final TwitchApi twitchApi;
    private final MessageExpressionParser commandParser;

    private boolean activeChat;
    private boolean recentRaid;
    private String previousId = "";
    
    public ScheduledMessageController(CommonUtils commonUtils, MessageExpressionParser messageExpressionParser) {
        twitchApi = commonUtils.getTwitchApi();
        socialSchedulerDb = commonUtils.getDbManager().getSocialSchedulerDb();
        commandParser = messageExpressionParser;
        activeChat = false;
        recentRaid = false;
        
        initScheduledMessages(commonUtils.getScheduler());
    }
    
    private void initScheduledMessages(ScheduledExecutorService scheduler) {
        long intervalLengthMillis = INTERVAL_LENGTH * 60 * 1000;
        LocalDateTime nowMinutes = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);
        long minutesToAdd = INTERVAL_LENGTH - (nowMinutes.getMinute() % INTERVAL_LENGTH);
        LocalDateTime nextInterval = nowMinutes.plusMinutes(minutesToAdd);
        
        scheduler.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                // only post if chat is active
                if (!activeChat) {
                    return;
                }
                activeChat = false;
                
                // only post if stream is live
                Stream stream;
                try {
                    stream = twitchApi.getStreamByUserId(twitchApi.getStreamerUser().getId());
                } catch (HystrixRuntimeException e) {
                    e.printStackTrace();
                    System.out.println("Error retrieving stream for SocialScheduler");
                    stream = null;
                }
                if (stream == null) {
                    return;
                }
                
                if (recentRaid) {
                    recentRaid = false;
                    postAfterRaidMsg();
                } else {
                    postRandomMsg();
                }
            }
        }, LocalDateTime.now().until(nextInterval, ChronoUnit.MILLIS), intervalLengthMillis, TimeUnit.MILLISECONDS);
    }

    private void postRandomMsg() {
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
    
    private void postAfterRaidMsg() {
        ScheduledMessage message = socialSchedulerDb.getMessage(FOLLOW_MESSAGE_ID);
        if (message == null) {
            System.out.println("Error posting scheduled follow message after a raid");
            return;
        }
        
        twitchApi.channelAnnouncement(commandParser.parse(message.getMessage()));
        previousId = message.getId();
    }
    
    @Override
    public void onRaid(ChannelRaidEvent raidEvent) {
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
