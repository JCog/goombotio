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

    private final String COLLECTION_NAME = "watchtime";
    private final String ID_KEY = "_id";
    private final String MINUTES_KEY = "minutes";
    private final String NAME_KEY = "name";


    public WatchTimeDb() {
        super();
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
                    .append(monthlyMinutesKey, minutes);
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

            updateOne(eq(ID_KEY, id), new Document("$set", new Document(MINUTES_KEY, newMinutes)));
            updateOne(eq(ID_KEY, id), new Document("$set", new Document(monthlyMinutesKey, newMonthlyMinutes)));
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

    public ArrayList<Map.Entry<String, Integer>> getTopUsers() {
        MongoCursor<Document> result = find().sort(Sorts.descending(MINUTES_KEY)).iterator();
        ArrayList<Map.Entry<String, Integer>> topUsers = new ArrayList<>();

        while (result.hasNext()) {
            Document doc = result.next();
            String name = doc.getString(NAME_KEY);
            int minutes = doc.getInteger(MINUTES_KEY);
            topUsers.add(new AbstractMap.SimpleEntry<String, Integer>(name, minutes));
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
        return String.format("points%d%d", year, month);
    }

    private String getMonthlyMinutesKey(int year, int month) {
        return String.format("points%d%d", year, month);
    }
}
