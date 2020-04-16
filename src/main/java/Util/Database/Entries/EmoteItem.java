package Util.Database.Entries;

public class EmoteItem {
    private final String pattern;
    private final int count;
    private final int users;
    
    public EmoteItem(String pattern, int count, int users) {
        this.pattern = pattern;
        this.count = count;
        this.users = users;
    }
    
    
    public String getPattern() {
        return pattern;
    }
    
    public int getCount() {
        return count;
    }
    
    public int getUsers() {
        return users;
    }
}
