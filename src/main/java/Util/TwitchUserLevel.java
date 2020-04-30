package Util;

import com.gikk.twirk.types.users.TwitchUser;

public class TwitchUserLevel {
    public enum USER_LEVEL {
        BROADCASTER(9),
        MOD(6),
        VIP(5),
        STAFF(4),
        SUBSCRIBER(2),
        DEFAULT(0);
    
        public final int value;
    
        USER_LEVEL(int value) {
            this.value = value;
        }
    }
    
    public static USER_LEVEL getUserLevel(TwitchUser twitchUser) {
        boolean broadcaster = false;
        boolean mod = false;
        boolean vip = false;
        boolean staff = false;
        boolean sub = false;
        for (String badgeString : twitchUser.getBadges()) {
            String badge = badgeString.split("/", 2)[0];
            switch (badge) {
                case ("broadcaster"):
                    broadcaster = true;
                    break;
                case ("moderator"):
                    mod = true;
                    break;
                case ("vip"):
                    vip = true;
                    break;
                case ("staff"):
                    staff = true;
                    break;
                case ("subscriber"):
                    sub = true;
                    break;
            }
        }
        if (broadcaster) {
            return USER_LEVEL.BROADCASTER;
        }
        else if (mod) {
            return USER_LEVEL.MOD;
        }
        else if (vip) {
            return USER_LEVEL.VIP;
        }
        else if (staff) {
            return USER_LEVEL.STAFF;
        }
        else if (sub) {
            return USER_LEVEL.SUBSCRIBER;
        }
        else {
            return USER_LEVEL.DEFAULT;
        }
    }
}
