package listeners.events;

import com.gikk.twirk.events.TwirkListener;
import com.gikk.twirk.types.usernotice.Usernotice;
import com.gikk.twirk.types.usernotice.subtype.Subscription;
import com.gikk.twirk.types.users.TwitchUser;
import org.apache.commons.lang.SystemUtils;
import util.FileWriter;
import util.TwitchApi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.TimerTask;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static java.lang.System.out;

public class SubListener implements TwirkListener {
    
    private static final String LOCAL_RECENT_SUB_FILENAME = "recent_sub.txt";
    
    private final TwitchApi twitchApi;
    private final ScheduledExecutorService scheduler;
    private final HashMap<String,Boolean> subTimersActive;
    private final HashMap<String,ArrayList<String>> giftedSubs;

    public SubListener(TwitchApi twitchApi, ScheduledExecutorService scheduler) {
        this.twitchApi = twitchApi;
        this.scheduler = scheduler;
        subTimersActive = new HashMap<>();
        giftedSubs = new HashMap<>();
    }

    @Override
    public void onUsernotice(TwitchUser user, Usernotice usernotice) {
        if (usernotice.isSubscription() && usernotice.getSubscription().isPresent()) {
            Subscription sub = usernotice.getSubscription().get();
            if (sub.isGift() && sub.getSubscriptionGift().isPresent()) {
                String gifterName = user.getDisplayName();
                String recipientName = sub.getSubscriptionGift().get().getRecipiantDisplayName();
                out.printf("%s has gifted a sub to %s%n", gifterName, recipientName);

                giftedSubs.computeIfAbsent(gifterName, k -> new ArrayList<>());
                giftedSubs.get(gifterName).add(recipientName);

                if (subTimersActive.get(gifterName) == null || !subTimersActive.get(gifterName)) {
                    subTimersActive.put(gifterName, true);
                    scheduler.schedule(new TimerTask() {

                        @Override
                        public void run() {
                            int subCount = giftedSubs.get(gifterName).size();
                            try {
                                TimeUnit.SECONDS.sleep(1);
                                int updatedSubCount = giftedSubs.get(gifterName).size();
                                while (subCount != updatedSubCount) {
                                    subCount = updatedSubCount;
                                    TimeUnit.SECONDS.sleep(1);
                                    updatedSubCount = giftedSubs.get(gifterName).size();
                                }
                            }
                            catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            if (giftedSubs.get(gifterName).size() == 1) {
                                twitchApi.channelMessage(String.format(
                                        "jcogChamp @%s Thank you for gifting a sub to @%s! jcogChamp",
                                        gifterName,
                                        recipientName));
                            }
                            else {
                                twitchApi.channelMessage(String.format(
                                        "jcogChamp @%s Thank you for the %d gift subs! jcogChamp",
                                        gifterName,
                                        subCount));
                            }

                            giftedSubs.remove(gifterName);
                            subTimersActive.put(gifterName, false);
                        }
                    }, 0, TimeUnit.SECONDS);
                }
            }
            else {
                int months = sub.getMonths();
                if (months == 1) {
                    twitchApi.channelMessage(String.format("jcogChamp @%s just subbed! Welcome to the Rookery™! jcogChamp",
                                                       user.getDisplayName()));
                }
                else {
                    twitchApi.channelMessage(String.format(
                            "jcogChamp @%s just resubbed for %d months! Welcome back to the Rookery™! jcogChamp",
                            user.getDisplayName(),
                            months));
                }
                outputRecentSubFile(user.getDisplayName());
            }
        }
    }
    
    private void outputRecentSubFile(String displayName) {
        FileWriter.writeToFile(getOutputLocation(), LOCAL_RECENT_SUB_FILENAME, displayName);
    }
    
    private String getOutputLocation() {
        if (SystemUtils.IS_OS_LINUX) {
            return "/srv/goombotio/";
        }
        return "output/";
    }

}
