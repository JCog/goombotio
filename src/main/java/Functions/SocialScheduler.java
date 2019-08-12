package Functions;

import com.gikk.twirk.Twirk;
import com.gikk.twirk.events.TwirkListener;
import com.gikk.twirk.types.twitchMessage.TwitchMessage;
import com.gikk.twirk.types.users.TwitchUser;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Random;
import java.util.Vector;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SocialScheduler {

    enum SocialType {
        DISCORD,
        INSTA,
        TWITTER,
        YOUTUBE
    }

    private final Twirk twirk;
    private Random random;
    private boolean running;
    private boolean activeChat;

    public SocialScheduler(Twirk twirk) {
        this.twirk = twirk;
        this.running = false;
        this.activeChat = false;
        random = new Random();
        twirk.addIrcListener(new AnyMessageListener());
    }

    public void start() {
        running = true;
        scheduleSocialMsgs();
    }

    public void stop() {
        running = false;
    }

    private void socialLoop() {
        if (running) {
            if (activeChat) {
                postTwoMsgs();
            }
            scheduleSocialMsgs();
            activeChat = false;
        }
    }

    private void postTwoMsgs() {
        Vector<SocialType> types = new Vector<>();
        types.add(SocialType.DISCORD);
        types.add(SocialType.INSTA);
        types.add(SocialType.TWITTER);
        types.add(SocialType.YOUTUBE);

        int index = random.nextInt(types.size());
        postMsg(types.elementAt(index));
        types.remove(index);

        index = random.nextInt(types.size());
        postMsg(types.elementAt(index));
    }

    private void postMsg(SocialType type) {
        switch (type) {
            case DISCORD:
                twirk.channelMessage("/me Want somewhere to hang out when I'm offline? Join the community Discord! discord.gg/B5b28M5");
                break;
            case INSTA:
                twirk.channelMessage("/me Follow me on Instagram! This is the best way to see what I'm getting up to. instagram.com/jcoggerr");
                break;
            case TWITTER:
                twirk.channelMessage("/me Follow me on Twitter! I post stream updates as well as whatever happens to be on my mind. twitter.com/JCog_");
                break;
            case YOUTUBE:
                twirk.channelMessage("/me Check out my YouTube channel! This is where I post all my highlights, PB VODs, and tutorials. youtube.jcoggers.com");
                break;
        }
    }

    private void scheduleSocialMsgs() {
        int intervalLength = 15;

        LocalDateTime now = LocalDateTime.now();
        int currentMinute = now.getMinute();
        int minutesToAdd = intervalLength - (currentMinute % intervalLength);
        LocalDateTime nextInterval = now.plusMinutes(minutesToAdd);

        ScheduledExecutorService ses = Executors.newScheduledThreadPool(1);
        ses.schedule(() -> socialLoop(),
                now.until(nextInterval, ChronoUnit.MILLIS), TimeUnit.MILLISECONDS);
    }

    public class AnyMessageListener implements TwirkListener {
        @Override
        public void onPrivMsg(TwitchUser sender, TwitchMessage message) {
            if (!sender.isOwner() && !sender.getUserName().toLowerCase().equals("goombotio")) {
                activeChat = true;
            }
        }
    }
}
