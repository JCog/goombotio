package database.stats;

import com.github.twitch4j.common.events.domain.EventUser;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Sorts;
import database.GbCollection;
import database.GbDatabase;
import database.entries.WatchtimeItem;
import org.bson.Document;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class WatchTimeDb extends GbCollection {
    private static final String COLLECTION_NAME = "watchtime";

    private static final String MINUTES_KEY = "minutes";
    private static final String NAME_KEY = "name";
    private static final String FIRST_SEEN_KEY = "first_seen";
    private static final String LAST_SEEN_KEY = "last_seen";


    public WatchTimeDb(GbDatabase gbDatabase) {
        super(gbDatabase, COLLECTION_NAME);
    }

    public void addMinutes(String id, String name, int minutes) {
        long idLong;
        try {
            idLong = Long.parseLong(id);
        } catch (NumberFormatException nfe) {
            System.out.printf(
                    "Error: ID \"%s\" (%s, %d minutes) cannot be parsed into long%n",
                    id,
                    name,
                    minutes
            );
            return;
        }
        Document result = findFirstEquals(ID_KEY, idLong);
        String monthlyMinutesKey = getMonthlyMinutesKey();

        if (result == null) {
            Document document = new Document(ID_KEY, idLong)
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

            updateOne(idLong, new Document(NAME_KEY, name));
            updateOne(idLong, new Document(MINUTES_KEY, newMinutes));
            updateOne(idLong, new Document(monthlyMinutesKey, newMonthlyMinutes));
            updateOne(idLong, new Document(LAST_SEEN_KEY, getDate()));
        }
    }

    @Nullable
    public WatchtimeItem getWatchtimeItem(String id) {
        Document result = findFirstEquals(ID_KEY, id);
        if (result != null) {
            return new WatchtimeItem(
                    result.getLong(ID_KEY),
                    result.getString(NAME_KEY),
                    result.getInteger(MINUTES_KEY),
                    result.getDate(FIRST_SEEN_KEY),
                    result.getDate(LAST_SEEN_KEY)
            );
        }
        return null;
    }

    public String getNameById(long id) {
        Document result = findFirstEquals(ID_KEY, id);
        if (result != null) {
            return result.getString(NAME_KEY);
        }
        return "";
    }

    public int getMinutesByEventUser(EventUser user) {
        return getMinutesById(Long.parseLong(user.getId()));
    }

    public int getMinutesByUsername(String username) {
        Document result = findFirstEquals(NAME_KEY, username.toLowerCase());
        if (result != null) {
            return result.getInteger(MINUTES_KEY);
        }
        return 0;
    }

    public int getMinutesById(long id) {
        Document result = findFirstEquals(ID_KEY, id);
        if (result != null) {
            return result.getInteger(MINUTES_KEY);
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
    public Date getFirstSeenById(long userId) {
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
    public Date getLastSeenById(long id) {
        Document result = findFirstEquals(ID_KEY, id);
        if (result != null) {
            return result.getDate(LAST_SEEN_KEY);
        }
        return null;
    }

    public ArrayList<Map.Entry<String,Integer>> getTopUsers() {
        MongoCursor<Document> result = findAll().sort(Sorts.descending(MINUTES_KEY)).iterator();
        ArrayList<Map.Entry<String,Integer>> topUsers = new ArrayList<>();

        while (result.hasNext()) {
            Document doc = result.next();
            String name = doc.getString(NAME_KEY);
            int minutes = doc.getInteger(MINUTES_KEY);
            topUsers.add(new AbstractMap.SimpleEntry<>(name, minutes));
        }
        return topUsers;
    }

    public HashSet<Long> getAllUserIds() {
        MongoCursor<Document> result = findAll().iterator();
        HashSet<Long> users = new HashSet<>();

        while (result.hasNext()) {
            Document document = result.next();
            users.add(document.getLong(ID_KEY));
        }
        return users;
    }

    public Vector<String> getTopMonthlyUsers() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        return getTopMonthlyUsers(year, month);
    }

    public Vector<String> getTopMonthlyUsers(int year, int month) {
        String monthlyMinutesKey = getMonthlyMinutesKey(year, month);
        MongoCursor<Document> result = findContainsKey(monthlyMinutesKey)
                .sort(Sorts.descending(monthlyMinutesKey)).iterator();
        Vector<String> topUsers = new Vector<>();

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

    public Vector<String> getMatchingUsers(String search) {
        Vector<String> result = new Vector<>();

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
