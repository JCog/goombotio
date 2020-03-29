package Util.Database;

import Util.Database.Entries.ViewerQueueEntry;
import com.mongodb.client.MongoCollection;
import org.bson.Document;

import static com.mongodb.client.model.Filters.eq;

public class ViewerQueueDb extends CollectionBase {
    private static ViewerQueueDb instance = null;
    
    private final String COLLECTION_NAME_KEY = "viewerqueue";
    private final String ID_KEY = "_id";
    private final String TOTAL_SESSIONS_KEY = "totalSessions";
    private final String ATTEMPTS_SINCE_LAST_SESSION_KEY = "attemptsSinceLastSession";
    private final String LAST_SESSION_ID_KEY = "lastSessionId";
    
    private final String SESSION = "session";
    private final String SESSION_ID_KEY = "sessionId";
    
    private int sessionId;
    
    private ViewerQueueDb() {
        super();
        sessionId = -1;
        Document result = find(eq(ID_KEY, SESSION)).first();
        if (result != null) {
            sessionId = result.getInteger(SESSION_ID_KEY);
        }
    }
    
    public static ViewerQueueDb getInstance() {
        if (instance == null) {
            instance = new ViewerQueueDb();
        }
        return instance;
    }
    
    public void incrementSessionId() {
        sessionId++;
        updateOne(eq(ID_KEY, SESSION), new Document("$set", new Document(SESSION_ID_KEY, sessionId)));
    }
    
    @Override
    protected MongoCollection<Document> setCollection() {
        return goombotioDb.getCollection(COLLECTION_NAME_KEY);
    }
    
    public void setUserJoined(long userId) {
        Document result = find(eq(ID_KEY, userId)).first();
        
        if (result == null) {
            Document document = new Document(ID_KEY, userId)
                    .append(TOTAL_SESSIONS_KEY, 1)
                    .append(ATTEMPTS_SINCE_LAST_SESSION_KEY, 0)
                    .append(LAST_SESSION_ID_KEY, sessionId);
            insertOne(document);
        }
        else {
            int newTotalSessions = result.getInteger(TOTAL_SESSIONS_KEY) + 1;
            updateOne(eq(ID_KEY, userId), new Document("$set", new Document(TOTAL_SESSIONS_KEY, newTotalSessions)));
            updateOne(eq(ID_KEY, userId), new Document("$set", new Document(ATTEMPTS_SINCE_LAST_SESSION_KEY, 0)));
            updateOne(eq(ID_KEY, userId), new Document("$set", new Document(LAST_SESSION_ID_KEY, sessionId)));
        }
    }
    
    public void setUserAttempted(long userId) {
        Document result = find(eq(ID_KEY, userId)).first();
    
        if (result == null) {
            Document document = new Document(ID_KEY, userId)
                    .append(TOTAL_SESSIONS_KEY, 0)
                    .append(ATTEMPTS_SINCE_LAST_SESSION_KEY, 1)
                    .append(LAST_SESSION_ID_KEY, -1);
            insertOne(document);
        }
        else {
            int newAttempts = result.getInteger(ATTEMPTS_SINCE_LAST_SESSION_KEY) + 1;
            updateOne(eq(ID_KEY, userId), new Document("$set", new Document(ATTEMPTS_SINCE_LAST_SESSION_KEY, newAttempts)));
        }
    }
    
    public ViewerQueueEntry getUser(long userId) {
        Document result = find(eq(ID_KEY, userId)).first();
        
        if (result == null) {
            Document document = new Document(ID_KEY, userId)
                    .append(TOTAL_SESSIONS_KEY, 0)
                    .append(ATTEMPTS_SINCE_LAST_SESSION_KEY, 0)
                    .append(LAST_SESSION_ID_KEY, -1);
            insertOne(document);
            return new ViewerQueueEntry(userId, 0, 0, -1);
        }
        else {
            int totalSessions = result.getInteger(TOTAL_SESSIONS_KEY);
            int attempts = result.getInteger(ATTEMPTS_SINCE_LAST_SESSION_KEY);
            int lastSessionId = result.getInteger(LAST_SESSION_ID_KEY);
            return new ViewerQueueEntry(userId, totalSessions, attempts, lastSessionId);
        }
    }
}
