package util;

import org.jetbrains.annotations.Nullable;

import java.util.Set;

public class TwitchUserLevel {
    public enum USER_LEVEL {
        BROADCASTER(9, "broadcaster"),
        MOD(6, "mod"),
        VIP(5, "vip"),
        STAFF(4, "staff"),
        SUBSCRIBER(2, "sub"),
        DEFAULT(0, "default");

        public final int value;
        public final String name;

        USER_LEVEL(int value, String name) {
            this.value = value;
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public static USER_LEVEL getUserLevel(Set<String> badges) {
        boolean broadcaster = false;
        boolean mod = false;
        boolean vip = false;
        boolean staff = false;
        boolean sub = false;
        for (String badgeString : badges) {
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
                case ("sub"):
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

    @Nullable
    public static USER_LEVEL getUserLevel(String type) {
        if (type.equals(USER_LEVEL.DEFAULT.toString())) {
            return USER_LEVEL.DEFAULT;
        }
        else if (type.equals(USER_LEVEL.SUBSCRIBER.toString()) || type.equals("sub")) {
            return USER_LEVEL.SUBSCRIBER;
        }
        else if (type.equals(USER_LEVEL.STAFF.toString())) {
            return USER_LEVEL.STAFF;
        }
        else if (type.equals(USER_LEVEL.VIP.toString())) {
            return USER_LEVEL.VIP;
        }
        else if (type.equals(USER_LEVEL.MOD.toString())) {
            return USER_LEVEL.MOD;
        }
        else if (type.equals(USER_LEVEL.BROADCASTER.toString())) {
            return USER_LEVEL.BROADCASTER;
        }
        else {
            return null;
        }
    }
}
