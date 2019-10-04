package Util.Database;

import com.gikk.twirk.types.users.TwitchUser;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Sorts;
import org.bson.Document;

import java.util.*;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.exists;

public class WatchTimeDb extends CollectionBase {
    private static WatchTimeDb instance = null;
    
    private final String COLLECTION_NAME = "watchtime";
    private final String ID_KEY = "_id";
    private final String MINUTES_KEY = "minutes";
    private final String NAME_KEY = "name";
    private final String FIRST_SEEN_KEY = "first_seen";
    private final String LAST_SEEN_KEY = "last_seen";


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

    public void addMinutes(long id, String name, int minutes) {
        Document result = find(eq(ID_KEY, id)).first();
        String monthlyMinutesKey = getMonthlyMinutesKey();

        if (result == null) {
            Document document = new Document(ID_KEY, id)
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
    
            updateOne(eq(ID_KEY, id), new Document("$set", new Document(NAME_KEY, name)));
            updateOne(eq(ID_KEY, id), new Document("$set", new Document(MINUTES_KEY, newMinutes)));
            updateOne(eq(ID_KEY, id), new Document("$set", new Document(monthlyMinutesKey, newMonthlyMinutes)));
            updateOne(eq(ID_KEY, id), new Document("$set", new Document(LAST_SEEN_KEY, getDate())));
        }
    }

    public int getMinutes(TwitchUser user) {
        long id = user.getUserID();

        Document result = find(eq(ID_KEY, id)).first();
        if (result != null) {
            return result.getInteger(MINUTES_KEY);
        }
        return 0;
    }
    
    public Date getFirstSeen(String username) {
        String userLower = username.toLowerCase();
        
        Document result = find(eq(NAME_KEY, userLower)).first();
        if (result != null) {
            return result.getDate(FIRST_SEEN_KEY);
        }
        return getDate();
    }

    public ArrayList<Map.Entry<String, Integer>> getTopUsers() {
        MongoCursor<Document> result = find().sort(Sorts.descending(MINUTES_KEY)).iterator();
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
        MongoCursor<Document> result = find(exists(monthlyMinutesKey)).sort(Sorts.descending(monthlyMinutesKey)).iterator();
        Vector<String> topUsers = new Vector<>();

        while (result.hasNext()) {
            topUsers.add(result.next().getString(NAME_KEY));
        }
        return topUsers;
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
