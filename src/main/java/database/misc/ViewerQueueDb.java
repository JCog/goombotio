package database.misc;

import database.GbCollection;
import database.GbDatabase;
import org.bson.Document;

public class ViewerQueueDb extends GbCollection {
    private static final String COLLECTION_NAME = "viewerqueue";

    private static final String TOTAL_SESSIONS_KEY = "totalSessions";
    private static final String ATTEMPTS_SINCE_LAST_SESSION_KEY = "attemptsSinceLastSession";
    private static final String LAST_SESSION_ID_KEY = "lastSessionId";
    private static final String SESSION = "session";
    private static final String SESSION_ID_KEY = "sessionId";

    private int sessionId;

    public ViewerQueueDb(GbDatabase gbDatabase) {
        super(gbDatabase, COLLECTION_NAME);
        sessionId = -1;
        Document result = findFirstEquals(ID_KEY, SESSION);
        if (result != null) {
            sessionId = result.getInteger(SESSION_ID_KEY);
        }
    }

    public void incrementSessionId() {
        sessionId++;
        updateOne(SESSION, new Document(SESSION_ID_KEY, sessionId));
    }

    public void setUserJoined(long userId) {
        Document result = findFirstEquals(ID_KEY, userId);

        if (result == null) {
            Document document = new Document(ID_KEY, userId)
                    .append(TOTAL_SESSIONS_KEY, 1)
                    .append(ATTEMPTS_SINCE_LAST_SESSION_KEY, 0)
                    .append(LAST_SESSION_ID_KEY, sessionId);
            insertOne(document);
        } else {
            int newTotalSessions = result.getInteger(TOTAL_SESSIONS_KEY) + 1;
            updateOne(userId, new Document(TOTAL_SESSIONS_KEY, newTotalSessions));
            updateOne(userId, new Document(ATTEMPTS_SINCE_LAST_SESSION_KEY, 0));
            updateOne(userId, new Document(LAST_SESSION_ID_KEY, sessionId));
        }
    }

    public void setUserAttempted(long userId) {
        Document result = findFirstEquals(ID_KEY, userId);

        if (result == null) {
            Document document = new Document(ID_KEY, userId)
                    .append(TOTAL_SESSIONS_KEY, 0)
                    .append(ATTEMPTS_SINCE_LAST_SESSION_KEY, 1)
                    .append(LAST_SESSION_ID_KEY, -1);
            insertOne(document);
        } else {
            int newAttempts = result.getInteger(ATTEMPTS_SINCE_LAST_SESSION_KEY) + 1;
            updateOne(userId, new Document(ATTEMPTS_SINCE_LAST_SESSION_KEY, newAttempts));
        }
    }

    public ViewerQueueEntry getUser(long userId) {
        Document result = findFirstEquals(ID_KEY, userId);

        if (result == null) {
            Document document = new Document(ID_KEY, userId)
                    .append(TOTAL_SESSIONS_KEY, 0)
                    .append(ATTEMPTS_SINCE_LAST_SESSION_KEY, 0)
                    .append(LAST_SESSION_ID_KEY, -1);
            insertOne(document);
            return new ViewerQueueEntry(userId, 0, 0, -1);
        } else {
            int totalSessions = result.getInteger(TOTAL_SESSIONS_KEY);
            int attempts = result.getInteger(ATTEMPTS_SINCE_LAST_SESSION_KEY);
            int lastSessionId = result.getInteger(LAST_SESSION_ID_KEY);
            return new ViewerQueueEntry(userId, totalSessions, attempts, lastSessionId);
        }
    }
    
    public static class ViewerQueueEntry {
        public final long id;
        public final int totalSessions;
        public final int attemptsSinceLastSession;
        public final int lastSessionId;
        public final boolean subbed;
        public final String username;
        
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
}
