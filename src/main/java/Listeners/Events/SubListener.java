package Listeners.Events;

import com.gikk.twirk.Twirk;
import com.gikk.twirk.events.TwirkListener;
import com.gikk.twirk.types.usernotice.Usernotice;
import com.gikk.twirk.types.usernotice.subtype.Subscription;
import com.gikk.twirk.types.users.TwitchUser;

import java.util.*;

import static java.lang.System.out;

public class SubListener implements TwirkListener {
    private Twirk twirk;
    private HashMap<String, Boolean> subTimersActive;
    private HashMap<String, ArrayList<String>> giftedSubs;
    
    public SubListener(Twirk twirk){
        this.twirk = twirk;
        subTimersActive = new HashMap<>();
        giftedSubs = new HashMap<>();
    }
    
    @Override
    public void onUsernotice(TwitchUser user, Usernotice usernotice) {
        if (usernotice.isSubscription()) {
            Subscription sub = usernotice.getSubscription().get();
            if (sub.isGift()) {
                String gifterName = user.getDisplayName();
                String recipientName = sub.getSubscriptionGift().get().getRecipiantDisplayName();
                out.println(String.format("%s has gifted a sub to %s", gifterName, recipientName));
    
                giftedSubs.computeIfAbsent(gifterName, k -> new ArrayList<>());
                giftedSubs.get(gifterName).add(recipientName);
                
                if (subTimersActive.get(gifterName) == null || !subTimersActive.get(gifterName)) {
                    subTimersActive.put(gifterName, true);
                    new Timer().schedule(new TimerTask() {
                        
                        @Override
                        public void run() {
                            if (giftedSubs.get(gifterName).size() == 1) {
                                twirk.channelMessage(String.format("jcogChamp @%s Thank you for gifting a sub to @%s! jcogChamp", gifterName, recipientName));
                            }
                            else {
                                twirk.channelMessage(String.format("jcogChamp @%s Thank you for the %d gift subs! jcogChamp", gifterName, giftedSubs.get(gifterName).size()));
                            }
                            
                            giftedSubs.remove(gifterName);
                            subTimersActive.put(gifterName, false);
                        }
                    }, 500);
                }
            }
            else {
                int months = sub.getMonths();
                if (months == 1) {
                    twirk.channelMessage(String.format("jcogChamp @%s just subbed! Welcome to the Rookery™! jcogChamp", user.getDisplayName()));
                }
                else {
                    twirk.channelMessage(String.format("jcogChamp @%s just resubbed for %d months! Welcome back to the Rookery™! jcogChamp", user.getDisplayName(), months));
                }
            }
        }
    }
    
}
