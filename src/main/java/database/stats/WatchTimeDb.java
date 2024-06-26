package database.stats;

import com.github.twitch4j.common.events.domain.EventUser;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Sorts;
import database.GbCollection;
import database.GbDatabase;
import org.bson.Document;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class WatchTimeDb extends GbCollection {
    private static final String COLLECTION_NAME = "watchtime";

    private static final String MINUTES_KEY = "minutes";
    private static final String NAME_KEY = "name";
    private static final String FIRST_SEEN_KEY = "first_seen";
    private static final String LAST_SEEN_KEY = "last_seen";
    
    public record WatchtimeItem(String id, String name, int minutes, Date firstSeen, Date lastSeen) {}
    
    public WatchTimeDb(GbDatabase gbDatabase) {
        super(gbDatabase, COLLECTION_NAME);
    }

    public void addMinutes(String id, String name, int minutes) {
        Document result = findFirstEquals(ID_KEY, id);
        String monthlyMinutesKey = getMonthlyMinutesKey();

        if (result == null) {
            Document document = new Document(ID_KEY, id)
                    .append(NAME_KEY, name)
                    .append(MINUTES_KEY, minutes)
                    .append(monthlyMinutesKey, minutes)
                    .append(FIRST_SEEN_KEY, getDate())
                    .append(LAST_SEEN_KEY, getDate());
            insertOne(document);
        } else {
            int newMinutes = result.getInteger(MINUTES_KEY) + minutes;
            int newMonthlyMinutes;
            if (result.get(monthlyMinutesKey) == null) {
                newMonthlyMinutes = 0;
            } else {
                newMonthlyMinutes = result.getInteger(monthlyMinutesKey);
            }
            newMonthlyMinutes += minutes;

            updateOne(id, new Document(NAME_KEY, name));
            updateOne(id, new Document(MINUTES_KEY, newMinutes));
            updateOne(id, new Document(monthlyMinutesKey, newMonthlyMinutes));
            updateOne(id, new Document(LAST_SEEN_KEY, getDate()));
        }
    }

    @Nullable
    public WatchtimeItem getWatchtimeItem(String id) {
        Document result = findFirstEquals(ID_KEY, id);
        if (result != null) {
            return new WatchtimeItem(
                    result.getString(ID_KEY),
                    result.getString(NAME_KEY),
                    result.getInteger(MINUTES_KEY),
                    result.getDate(FIRST_SEEN_KEY),
                    result.getDate(LAST_SEEN_KEY)
            );
        }
        return null;
    }

    public String getNameById(String id) {
        Document result = findFirstEquals(ID_KEY, id);
        if (result != null) {
            return result.getString(NAME_KEY);
        }
        return "";
    }

    public int getMinutesByEventUser(EventUser user) {
        return getMinutesById(user.getId());
    }

    public int getMinutesByUsername(String username) {
        Document result = findFirstEquals(NAME_KEY, username.toLowerCase());
        if (result != null) {
            return result.getInteger(MINUTES_KEY);
        }
        return 0;
    }

    public int getMinutesById(String id) {
        Document result = findFirstEquals(ID_KEY, id);
        if (result != null) {
            return result.getInteger(MINUTES_KEY);
        }
        return 0;
    }
    
    public int getMonthlyMinutesByUsername(String username) {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        return getMonthlyMinutesByUsername(username, year, month);
    }
    public int getMonthlyMinutesByUsername(String username, int year, int month) {
        String userLower = username.toLowerCase();
        String monthlyMinutesKey = getMonthlyMinutesKey(year, month);
        
        Document result = findFirstEquals(NAME_KEY, userLower);
        if (result != null) {
            Integer minutes = result.getInteger(monthlyMinutesKey);
            if (minutes != null) {
                return minutes;
            }
        }
        return 0;
    }

    @Nullable
    public Date getFirstSeenByUsername(String username) {
        String userLower = username.toLowerCase();

        Document result = findFirstEquals(NAME_KEY, userLower);
        if (result != null) {
            return result.getDate(FIRST_SEEN_KEY);
        }
        return null;
    }

    @Nullable
    public Date getFirstSeenById(String userId) {
        Document result = findFirstEquals(ID_KEY, userId);
        if (result != null) {
            return result.getDate(FIRST_SEEN_KEY);
        }
        return null;
    }

    @Nullable
    public Date getLastSeenByUsername(String username) {
        String userLower = username.toLowerCase();

        Document result = findFirstEquals(NAME_KEY, userLower);
        if (result != null) {
            return result.getDate(LAST_SEEN_KEY);
        }
        return null;
    }

    @Nullable
    public Date getLastSeenById(String id) {
        Document result = findFirstEquals(ID_KEY, id);
        if (result != null) {
            return result.getDate(LAST_SEEN_KEY);
        }
        return null;
    }

    public List<Map.Entry<String,Integer>> getTopUsers() {
        MongoCursor<Document> result = findAll().sort(Sorts.descending(MINUTES_KEY)).iterator();
        List<Map.Entry<String,Integer>> topUsers = new ArrayList<>();

        while (result.hasNext()) {
            Document doc = result.next();
            String name = doc.getString(NAME_KEY);
            int minutes = doc.getInteger(MINUTES_KEY);
            topUsers.add(new AbstractMap.SimpleEntry<>(name, minutes));
        }
        return topUsers;
    }

    public Set<String> getAllUserIds() {
        MongoCursor<Document> result = findAll().iterator();
        Set<String> users = new HashSet<>();

        while (result.hasNext()) {
            Document document = result.next();
            users.add(document.getString(ID_KEY));
        }
        return users;
    }

    public List<String> getTopMonthlyUsers() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        return getTopMonthlyUsers(year, month);
    }

    public List<String> getTopMonthlyUsers(int year, int month) {
        String monthlyMinutesKey = getMonthlyMinutesKey(year, month);
        MongoCursor<Document> result = findContainsKey(monthlyMinutesKey)
                .sort(Sorts.descending(monthlyMinutesKey))
                .iterator();
        List<String> topUsers = new ArrayList<>();

        while (result.hasNext()) {
            topUsers.add(result.next().getString(NAME_KEY));
        }
        return topUsers;
    }

    public int getTotalMonthlyWatchtime() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        return getTotalMonthlyWatchtime(year, month);
    }

    public int getTotalMonthlyWatchtime(int year, int month) {
        int minutes = 0;
        String monthlyMinutesKey = getMonthlyMinutesKey(year, month);

        for (Document document : findContainsKey(monthlyMinutesKey)) {
            minutes += document.getInteger(monthlyMinutesKey);
        }
        return minutes;
    }

    public List<String> getMatchingUsers(String search) {
        List<String> result = new ArrayList<>();

        for (Document document : findAll()) {
            String name = document.getString(NAME_KEY);
            if (name.contains(search)) {
                result.add(name);
            }
        }
        return result;
    }

    private String getMonthlyMinutesKey() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        return String.format("minutes%d%d", year, month);
    }

    private String getMonthlyMinutesKey(int year, int month) {
        return String.format("minutes%d%d", year, month);
    }

    private static Date getDate() {
        Calendar date = new GregorianCalendar();
        date.set(Calendar.HOUR_OF_DAY, 12);
        date.set(Calendar.MINUTE, 0);
        date.set(Calendar.SECOND, 0);
        date.set(Calendar.MILLISECOND, 0);
        return date.getTime();
    }
}
