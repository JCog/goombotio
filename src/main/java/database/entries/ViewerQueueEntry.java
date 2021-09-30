package database.entries;

public class ViewerQueueEntry {
    public long id;
    public int totalSessions;
    public int attemptsSinceLastSession;
    public int lastSessionId;
    public boolean subbed;
    public String username;

    public ViewerQueueEntry(long id, int totalSessions, int attemptsSinceLastSession, int lastSessionId) {
        this.id = id;
        this.totalSessions = totalSessions;
        this.attemptsSinceLastSession = attemptsSinceLastSession;
        this.lastSessionId = lastSessionId;
        subbed = false;
        username = "";
    }

    /*
    Username, ID, Subbed, Attempts, Total Sessions, Last Session ID
     */
    @Override
    public String toString() {
        return String.format("%s, %d, %b, %d, %d, %d",
                username, id, subbed, attemptsSinceLastSession, totalSessions, lastSessionId);
    }
}