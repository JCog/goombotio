package Util.Database;

import com.gikk.twirk.types.users.TwitchUser;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Sorts;
import org.bson.Document;

import java.util.Vector;

import static com.mongodb.client.model.Filters.eq;

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

        if (result == null) {
            Document document = new Document(ID_KEY, id)
                    .append(NAME_KEY, name)
                    .append(MINUTES_KEY, minutes);
            insertOne(document);
        }
        else {
            int newMinutes = (int)result.get(MINUTES_KEY) + minutes;
            updateOne(eq(ID_KEY, id), new Document("$set", new Document(MINUTES_KEY, newMinutes)));
        }
    }

    public int getMinutes(TwitchUser user) {
        long id = user.getUserID();

        Document result = find(eq(ID_KEY, id)).first();
        if (result != null) {
            return (int)result.get(MINUTES_KEY);
        }
        return 0;
    }

    public Vector<String> getTopUsers() {
        MongoCursor<Document> result = find().sort(Sorts.descending(MINUTES_KEY)).iterator();
        Vector<String> topUsers = new Vector<>();

        while (result.hasNext()) {
            topUsers.add((String)result.next().get(NAME_KEY));
        }
        return topUsers;
    }
}