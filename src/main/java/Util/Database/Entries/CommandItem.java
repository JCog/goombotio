package Util.Database.Entries;

import Util.TwitchUserLevel;

public class CommandItem {
    
    private final String id;
    private final String message;
    private final TwitchUserLevel.USER_LEVEL permission;
    private final long cooldown;
    private final int count;
    
    public static TwitchUserLevel.USER_LEVEL getUserLevel(int permission) {
        switch (permission) {
            case 0:
                return TwitchUserLevel.USER_LEVEL.DEFAULT;
            case 2:
                return TwitchUserLevel.USER_LEVEL.SUBSCRIBER;
            case 4:
                return TwitchUserLevel.USER_LEVEL.STAFF;
            case 5:
                return TwitchUserLevel.USER_LEVEL.VIP;
            case 6:
                return TwitchUserLevel.USER_LEVEL.MOD;
            case 9:
                return TwitchUserLevel.USER_LEVEL.BROADCASTER;
            default:
                return null;
        }
    }
    
    public CommandItem(String id, String message, TwitchUserLevel.USER_LEVEL permission, long cooldown, int count) {
        this.id = id;
        this.message = message;
        this.permission = permission;
        this.cooldown = cooldown;
        this.count = count;
    }
    
    public String getId() {
        return id;
    }
    
    public String getMessage() {
        return message;
    }
    
    public TwitchUserLevel.USER_LEVEL getPermission() {
        return permission;
    }
    
    public long getCooldown() {
        return cooldown;
    }
    
    public int getCount() {
        return count;
    }
}
