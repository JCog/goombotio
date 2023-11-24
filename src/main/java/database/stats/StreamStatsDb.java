package database.stats;

import com.github.twitch4j.helix.domain.User;
import database.GbCollection;
import database.GbDatabase;
import org.bson.Document;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

public class StreamStatsDb extends GbCollection {
    private static final String COLLECTION_NAME = "streamstats";

    private static final String START_KEY = "start_time";
    private static final String END_KEY = "end_time";
    private static final String VIEW_COUNTS_KEY = "view_counts";
    private static final String USER_LIST_KEY = "user_list";
    private static final String USER_ID = "user_id";
    private static final String USERNAME_KEY = "username";
    private static final String MINUTES_KEY = "minutes";
    private static final String NEW_USER_KEY = "new_user";
    private static final String NEWEST_KEY = "newest_key";
    private static final String NEWEST_ID = "newest";


    /**
     * Database interface to store statistics about individual streams
     */
    public StreamStatsDb(GbDatabase gbDatabase) {
        super(gbDatabase, COLLECTION_NAME);
    }

    /**
     * Adds a stream to the database
     *
     * @param startTime        time the stream started
     * @param endTime          time the stream ended
     * @param viewerCounts     array of the viewer counts for every minute of the stream
     * @param userIdMinutesMap map of user IDs and how long they spent watching each stream
     * @param twitchUserList   list of users from Twitch's API
     */
    public void addStream(
            WatchTimeDb watchTimeDb,
            Date startTime,
            Date endTime,
            List<Integer> viewerCounts,
            Map<String,Integer> userIdMinutesMap,
            List<User> twitchUserList
    ) {
        String streamKey = getNewStreamKey();
        Document document = new Document(ID_KEY, streamKey)
                .append(START_KEY, startTime)
                .append(END_KEY, endTime)
                .append(VIEW_COUNTS_KEY, viewerCounts);
        
        Map<String, String> userIdNameMap = twitchUserList.stream().collect(
                Collectors.toMap(User::getId, User::getDisplayName)
        );
        
        List<Document> userList = new ArrayList<>();
        for (Map.Entry<String,Integer> entry : userIdMinutesMap.entrySet()) {
            String userId = entry.getKey();
            int minutes = entry.getValue();
            String username = userIdNameMap.get(userId);
            boolean newUser = watchTimeDb.getMinutesById(Long.parseLong(userId)) == 0;
            userList.add(
                    new Document(USER_ID, userId)
                            .append(USERNAME_KEY, username)
                            .append(MINUTES_KEY, minutes)
                            .append(NEW_USER_KEY, newUser)
            );
        }
        document.append(USER_LIST_KEY, userList);

        //add stream
        Document stream = findFirstEquals(ID_KEY, streamKey);
        if (stream == null) {
            insertOne(document);
        } else {
            updateOne(streamKey, document);
        }

        //update newest stream key
        Document key = findFirstEquals(ID_KEY, NEWEST_ID);
        if (key == null) {
            insertOne(new Document(ID_KEY, NEWEST_ID).append(NEWEST_KEY, streamKey));
        } else {
            updateOne(NEWEST_ID, new Document(NEWEST_KEY, streamKey));
        }
    }

    /**
     * returns the start time of the last stream
     *
     * @return start time
     */
    @Nullable
    public Date getStreamStartTime() {
        String streamKey = getNewestStreamKey();
        Document result = findFirstEquals(ID_KEY, streamKey);

        if (result != null) {
            return result.getDate(START_KEY);
        }
        return null;
    }


    /**
     * returns the end time of the last stream
     *
     * @return end time
     */
    @Nullable
    public Date getStreamEndTime() {
        String streamKey = getNewestStreamKey();
        Document result = findFirstEquals(ID_KEY, streamKey);

        if (result != null) {
            return result.getDate(END_KEY);
        }
        return null;
    }

    /**
     * Returns a map of the watchtime for each user from the last stream
     *
     * @return user watch time map
     */
    public Map<String,Integer> getUserMinutesList() {
        String streamKey = getNewestStreamKey();
        List<Document> userListDocs = findFirstEquals(ID_KEY, streamKey).getList(USER_LIST_KEY, Document.class);
        Map<String,Integer> userListMap = new HashMap<>();

        for (Document userDoc : userListDocs) {
            String username = userDoc.getString(USERNAME_KEY);
            int minutes = userDoc.getInteger(MINUTES_KEY);
            userListMap.put(username, minutes);
        }

        return userListMap;
    }

    /**
     * Returns the list of users who watched the stream for the first time
     *
     * @return new user list
     */
    public List<String> getNewUserList() {
        String streamKey = getNewestStreamKey();
        List<Document> userListDocs = findFirstEquals(ID_KEY, streamKey).getList(USER_LIST_KEY, Document.class);
        List<String> userListOut = new ArrayList<>();

        for (Document userDoc : userListDocs) {
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
     *
     * @return returning user list
     */
    public List<String> getReturningUserList() {
        String streamKey = getNewestStreamKey();
        List<Document> userListDocs = findFirstEquals(ID_KEY, streamKey).getList(USER_LIST_KEY, Document.class);
        List<String> userListOut = new ArrayList<>();

        for (Document userDoc : userListDocs) {
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
     *
     * @return user list
     */
    public List<String> getUserList() {
        String streamKey = getNewestStreamKey();
        List<Document> userListDocs = findFirstEquals(ID_KEY, streamKey).getList(USER_LIST_KEY, Document.class);
        List<String> userListOut = new ArrayList<>();

        for (Document userDoc : userListDocs) {
            String username = userDoc.getString(USERNAME_KEY);
            userListOut.add(username);
        }

        return userListOut;
    }


    /**
     * Returns the array of viewer counts for every minute of the stream
     *
     * @return viewer counts
     */
    public List<Integer> getViewerCounts() {
        String streamKey = getNewestStreamKey();
        return findFirstEquals(ID_KEY, streamKey).getList(VIEW_COUNTS_KEY, Integer.class);
    }

    @Nullable
    private String getNewestStreamKey() {
        Document result = findFirstEquals(ID_KEY, NEWEST_ID);
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
