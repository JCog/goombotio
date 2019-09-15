package Listeners.Events;

import com.gikk.twirk.Twirk;
import com.gikk.twirk.events.TwirkListener;
import com.gikk.twirk.types.usernotice.Usernotice;
import com.gikk.twirk.types.usernotice.subtype.Subscription;
import com.gikk.twirk.types.users.TwitchUser;

import static java.lang.System.out;

public class SubListener implements TwirkListener {
    Twirk twirk;
    public SubListener(Twirk twirk){
        this.twirk = twirk;
    }
    
    @Override
    public void onUsernotice(TwitchUser user, Usernotice usernotice) {
        if (usernotice.isSubscription()) {
            Subscription sub = usernotice.getSubscription().get();
            if (sub.isGift()) {
                String recipientName = sub.getSubscriptionGift().get().getRecipiantDisplayName();
                out.println(String.format("%s has gifted a sub to %s", user.getDisplayName(), recipientName));
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
