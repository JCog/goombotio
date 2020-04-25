package Util.Database.Entries;

import com.gikk.twirk.enums.USER_TYPE;

public class CommandItem {
    
    private final String id;
    private final String message;
    private final USER_TYPE permission;
    private final long cooldown;
    private final int count;
    
    public static USER_TYPE getUserType(int permission) {
        switch (permission) {
            case 0:
                return USER_TYPE.DEFAULT;
            case 2:
                return USER_TYPE.SUBSCRIBER;
            case 4:
                return USER_TYPE.STAFF;
            case 6:
                return USER_TYPE.MOD;
            case 9:
                return USER_TYPE.OWNER;
            default:
                return null;
        }
    }
    
    public CommandItem(String id, String message, USER_TYPE permission, long cooldown, int count) {
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
    
    public USER_TYPE getPermission() {
        return permission;
    }
    
    public long getCooldown() {
        return cooldown;
    }
    
    public int getCount() {
        return count;
    }
}
