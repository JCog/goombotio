package functions;

import com.github.twitch4j.chat.events.channel.ChannelMessageEvent;
import com.github.twitch4j.common.events.domain.EventUser;
import com.github.twitch4j.helix.domain.Stream;
import com.github.twitch4j.helix.domain.User;
import com.netflix.hystrix.exception.HystrixRuntimeException;
import database.DbManager;
import database.entries.GenericMessage;
import database.entries.ScheduledMessage;
import database.misc.SocialSchedulerDb;
import listeners.TwitchEventListener;
import util.MessageExpressionParser;
import util.TwitchApi;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ScheduledMessageController {

    private final AnyMessageListener anyMessageListener = new AnyMessageListener();
    private final Random random = new Random();

    private final SocialSchedulerDb socialSchedulerDb;
    private final TwitchApi twitchApi;
    private final ScheduledExecutorService scheduler;
    private final MessageExpressionParser commandParser;
    private final User botUser;
    private final User streamerUser;
    private final int intervalLength;

    private boolean running;
    private boolean activeChat;
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
            User streamerUser,
            User botUser,
            int intervalLength
    ) {
        this.twitchApi = twitchApi;
        this.scheduler = scheduler;
        this.commandParser = messageExpressionParser;
        this.botUser = botUser;
        this.streamerUser = streamerUser;
        this.intervalLength = intervalLength;
        socialSchedulerDb = dbManager.getSocialSchedulerDb();
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
                stream = twitchApi.getStream(streamerUser.getLogin());
            } catch (HystrixRuntimeException e) {
                e.printStackTrace();
                System.out.println("Error retrieving stream for SocialScheduler");
                return;
            }
            if (activeChat && stream != null) {
                postRandomMsg();
            }
            scheduleSocialMessages();
            activeChat = true;
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
        String message = choices.get(selection).getMessage();
        twitchApi.channelMessage(commandParser.parse(new GenericMessage(message), null));
        previousId = choices.get(selection).getId();
    }

    private void scheduleSocialMessages() {
        LocalDateTime now = LocalDateTime.now();
        int currentMinute = now.getMinute();
        int minutesToAdd = intervalLength - (currentMinute % intervalLength);
        LocalDateTime nextInterval = now.plusMinutes(minutesToAdd);

        scheduler.schedule(this::socialLoop, now.until(nextInterval, ChronoUnit.MILLIS), TimeUnit.MILLISECONDS);
    }

    private class AnyMessageListener implements TwitchEventListener {
        @Override
        public void onPrivMsg(ChannelMessageEvent messageEvent) {
            EventUser sender = messageEvent.getUser();
            // chat is active as long as posters aren't the streamer or bot
            if (!sender.getId().equals(streamerUser.getId()) && !sender.getId().equals(botUser.getId())) {
                activeChat = true;
            }
        }
    }
}
