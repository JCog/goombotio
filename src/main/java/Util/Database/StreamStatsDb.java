package Util.Database;

import com.mongodb.client.MongoCollection;
import org.bson.Document;

import java.util.*;

import static com.mongodb.client.model.Filters.eq;

public class StreamStatsDb extends CollectionBase {
    private static StreamStatsDb instance = null;
    
    private WatchTimeDb watchTimeDb;
    
    private final String COLLECTION_NAME = "streamstats";
    private final String ID_KEY = "_id";
    private final String START_KEY = "start_time";
    private final String END_KEY = "end_time";
    private final String VIEW_COUNTS_KEY = "view_counts";
    private final String USER_LIST_KEY = "user_list";
    private final String USERNAME_KEY = "username";
    private final String MINUTES_KEY = "minutes";
    private final String NEW_USER_KEY = "new_user";
    
    private final String NEWEST_KEY = "newest_key";
    private final String NEWEST_ID = "newest";
    
    
    private StreamStatsDb() {
        super();
        watchTimeDb = WatchTimeDb.getInstance();
    }
    
    /**
     * Database interface to store statistics about individual streams
     */
    public static StreamStatsDb getInstance() {
        if (instance == null) {
            instance = new StreamStatsDb();
        }
        return instance;
    }

    @Override
    protected MongoCollection<Document> setCollection() {
        return goombotioDb.getCollection(COLLECTION_NAME);
    }
    
    /**
     * Adds a stream to the database
     * @param startTime time the stream started
     * @param endTime time the stream ended
     * @param viewerCounts array of the viewer counts for every minute of the stream
     * @param userMinutesMap map of users and how long they spent watching each stream
     */
    public void addStream(Date startTime, Date endTime, List<Integer> viewerCounts, HashMap<String, Integer> userMinutesMap) {
        String streamKey = getNewStreamKey();
        Document document = new Document(ID_KEY, streamKey)
                .append(START_KEY, startTime)
                .append(END_KEY, endTime)
                .append(VIEW_COUNTS_KEY, viewerCounts);
        
        List<Document> userList = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : userMinutesMap.entrySet()) {
            String username = entry.getKey();
            int minutes = entry.getValue() / (60 * 1000);
            boolean newUser = watchTimeDb.getMinutes(username) == 0;
            userList.add(new Document(USERNAME_KEY, username)
                    .append(MINUTES_KEY, minutes)
                    .append(NEW_USER_KEY, newUser)
            );
        }
        document.append(USER_LIST_KEY, userList);
        
        //add stream
        Document stream = find(eq(ID_KEY, streamKey)).first();
        if (stream == null) {
            insertOne(document);
        }
        else {
            updateOne(eq(ID_KEY, streamKey), new Document("$set", document));
        }
        
        //update newest stream key
        Document key = find(eq(ID_KEY, NEWEST_ID)).first();
        if (key == null) {
            insertOne(new Document(ID_KEY, NEWEST_ID).append(NEWEST_KEY, streamKey));
        }
        else {
            updateOne(eq(ID_KEY, NEWEST_ID), new Document("$set", new Document(NEWEST_KEY, streamKey)));
        }
    }
    
    /**
     * returns the start time of the last stream
     * @return start time
     */
    public Date getStreamStartTime() {
        String streamKey = getNewestStreamKey();
        Document result = find(eq(ID_KEY, streamKey)).first();
        
        if (result != null) {
            return result.getDate(START_KEY);
        }
        return null;
    }
    
    
    /**
     * returns the end time of the last stream
     * @return end time
     */
    public Date getStreamEndTime() {
        String streamKey = getNewestStreamKey();
        Document result = find(eq(ID_KEY, streamKey)).first();
        
        if (result != null) {
            return result.getDate(END_KEY);
        }
        return null;
    }
    
    /**
     * Returns a map of the watchtime for each user from the last stream
     * @return user watch time map
     */
    public HashMap<String, Integer> getUserMinutesList() {
        String streamKey = getNewestStreamKey();
        List<Document> userListDocs = find(eq(ID_KEY, streamKey)).first().getList(USER_LIST_KEY, Document.class);
        HashMap<String, Integer> userListMap = new HashMap<>();
        
        for(Document userDoc : userListDocs) {
            String username = userDoc.getString(USERNAME_KEY);
            int minutes = userDoc.getInteger(MINUTES_KEY);
            userListMap.put(username, minutes);
        }
        
        return userListMap;
    }
    
    /**
     * Returns the list of users who watched the stream for the first time
     * @return new user list
     */
    public List<String> getNewUserList() {
        String streamKey = getNewestStreamKey();
        List<Document> userListDocs = find(eq(ID_KEY, streamKey)).first().getList(USER_LIST_KEY, Document.class);
        List<String> userListOut = new ArrayList<>();
        
        for(Document userDoc : userListDocs) {
            String username = userDoc.getString(USERNAME_KEY);
            boolean newUser = userDoc.getBoolean(NEW_USER_KEY);
            if (newUser) {
                userListOut.add(username);
            }
        }
        
        return userListOut;
    }
    
    /**
     * Returns the list of users who watched the stream and have also watched at least one prior stream
     * @return returning user list
     */
    public List<String> getReturningUserList() {
        String streamKey = getNewestStreamKey();
        List<Document> userListDocs = find(eq(ID_KEY, streamKey)).first().getList(USER_LIST_KEY, Document.class);
        List<String> userListOut = new ArrayList<>();
        
        for(Document userDoc : userListDocs) {
            String username = userDoc.getString(USERNAME_KEY);
            boolean newUser = userDoc.getBoolean(NEW_USER_KEY);
            if (!newUser) {
                userListOut.add(username);
            }
        }
        
        return userListOut;
    }
    
    /**
     * Returns the list of users who watched the stream
     * @return user list
     */
    public List<String> getUserList() {
        String streamKey = getNewestStreamKey();
        List<Document> userListDocs = find(eq(ID_KEY, streamKey)).first().getList(USER_LIST_KEY, Document.class);
        List<String> userListOut = new ArrayList<>();
        
        for(Document userDoc : userListDocs) {
            String username = userDoc.getString(USERNAME_KEY);
            userListOut.add(username);
        }
        
        return userListOut;
    }
    
    
    /**
     * Returns the array of viewer counts for every minute of the stream
     * @return viewer counts
     */
    public List<Integer> getViewerCounts() {
        String streamKey = getNewestStreamKey();
        List<Integer> result = find(eq(ID_KEY, streamKey)).first().getList(VIEW_COUNTS_KEY, Integer.class);
        return result;
    }
    
    private String getNewestStreamKey() {
        Document result = find(eq(ID_KEY, NEWEST_ID)).first();
        if (result != null) {
            return result.getString(NEWEST_KEY);
        }
        return null;
    }
    
    private String getNewStreamKey() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        return String.format("stream%d-%d-%d-%d", year, month, day, hour);
    }
}
