package Util.Database;

import com.gikk.twirk.types.users.TwitchUser;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Sorts;
import org.bson.Document;

import java.util.*;

public class WatchTimeDb extends CollectionBase {
    
    private static final String COLLECTION_NAME = "watchtime";
    private static final String MINUTES_KEY = "minutes";
    private static final String NAME_KEY = "name";
    private static final String FIRST_SEEN_KEY = "first_seen";
    private static final String LAST_SEEN_KEY = "last_seen";
    
    private static WatchTimeDb instance = null;


    private WatchTimeDb() {
        super();
    }
    
    public static WatchTimeDb getInstance() {
        if (instance == null) {
            instance = new WatchTimeDb();
        }
        return instance;
    }

    @Override
    protected MongoCollection<Document> setCollection() {
        return goombotioDb.getCollection(COLLECTION_NAME);
    }

    public void addMinutes(String id, String name, int minutes) {
        long idLong;
        try {
            idLong = Long.parseLong(id);
        }
        catch (NumberFormatException nfe) {
            System.out.println(String.format("Error: ID \"%s\" (%s, %d minutes) cannot be parsed into long",
                    id, name, minutes));
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
        }
        else {
            int newMinutes = result.getInteger(MINUTES_KEY) + minutes;
            int newMonthlyMinutes;
            if (result.get(monthlyMinutesKey) == null) {
                newMonthlyMinutes = 0;
            }
            else {
                newMonthlyMinutes = result.getInteger(monthlyMinutesKey);
            }
            newMonthlyMinutes += minutes;
    
            updateOne(idLong, new Document(NAME_KEY, name));
            updateOne(idLong, new Document(MINUTES_KEY, newMinutes));
            updateOne(idLong, new Document(monthlyMinutesKey, newMonthlyMinutes));
            updateOne(idLong, new Document(LAST_SEEN_KEY, getDate()));
        }
    }
    
    public int getMinutes(TwitchUser user) {
        long id = user.getUserID();
        
        Document result = findFirstEquals(ID_KEY, id);
        if (result != null) {
            return result.getInteger(MINUTES_KEY);
        }
        return 0;
    }
    
    public int getMinutes(String username) {
        Document result = findFirstEquals(NAME_KEY, username.toLowerCase());
        if (result != null) {
            return result.getInteger(MINUTES_KEY);
        }
        return 0;
    }
    
    public Date getFirstSeen(String username) {
        String userLower = username.toLowerCase();
        
        Document result = findFirstEquals(NAME_KEY, userLower);
        if (result != null) {
            return result.getDate(FIRST_SEEN_KEY);
        }
        return getDate();
    }
    
    public Date getFirstSeen(long userId) {
        Document result = findFirstEquals(ID_KEY, userId);
        if (result != null) {
            return result.getDate(FIRST_SEEN_KEY);
        }
        return getDate();
    }
    
    public Date getLastSeen(String username) {
        String userLower = username.toLowerCase();
        
        Document result = findFirstEquals(NAME_KEY, userLower);
        if (result != null) {
            return result.getDate(LAST_SEEN_KEY);
        }
        return getDate();
    }

    public ArrayList<Map.Entry<String, Integer>> getTopUsers() {
        MongoCursor<Document> result = findAll().sort(Sorts.descending(MINUTES_KEY)).iterator();
        ArrayList<Map.Entry<String, Integer>> topUsers = new ArrayList<>();

        while (result.hasNext()) {
            Document doc = result.next();
            String name = doc.getString(NAME_KEY);
            int minutes = doc.getInteger(MINUTES_KEY);
            topUsers.add(new AbstractMap.SimpleEntry<>(name, minutes));
        }
        return topUsers;
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
