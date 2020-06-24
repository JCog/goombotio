package Database.Entries;

import java.util.Date;

public class WatchtimeItem {
    private final long id;
    private final String name;
    private final int minutes;
    private final Date firstSeen;
    private final Date lastSeen;
    
    public WatchtimeItem(long id, String name, int minutes, Date firstSeen, Date lastSeen) {
        this.id = id;
        this.name = name;
        this.minutes = minutes;
        this.firstSeen = firstSeen;
        this.lastSeen = lastSeen;
    }
    
    public long getId() {
        return id;
    }
    
    public String getName() {
        return name;
    }
    
    public int getMinutes() {
        return minutes;
    }
    
    public Date getFirstSeen() {
        return firstSeen;
    }
    
    public Date getLastSeen() {
        return lastSeen;
    }
}
