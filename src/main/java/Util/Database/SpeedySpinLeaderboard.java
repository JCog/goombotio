package Util.Database;

import com.gikk.twirk.types.users.TwitchUser;
import com.mongodb.client.MongoCollection;
import org.bson.Document;

import static com.mongodb.client.model.Filters.*;

public class SpeedySpinLeaderboard extends CollectionBase{

    private final String COLLECTION_NAME = "speedyspin";

    public SpeedySpinLeaderboard() {
        super();
    }

    @Override
    protected MongoCollection<Document> setCollection() {
        return goombotioDb.getCollection(COLLECTION_NAME);
    }

    public void addPointsAndWins(TwitchUser user, int points, int wins) {
        addPoints(user, points);
        addWins(user, wins);
    }

    public  void addPoints(TwitchUser user, int points) {
        long id = user.getUserID();
        String name = user.getDisplayName();

        Document result = find(eq("_id", id)).first();

        if (result == null) {
            Document document = new Document("_id", id)
                    .append("name", name)
                    .append("points", points)
                    .append("wins", 0);
            insertOne(document);
        }
        else {
            int newPoints = (int)result.get("points") + points;
            updateOne(eq("_id", id), new Document("$set", new Document("points", newPoints)));
        }
    }

    public void addWins(TwitchUser user, int wins) {
        long id = user.getUserID();
        String name = user.getDisplayName();

        Document result = find(eq("_id", id)).first();

        if (result == null) {
            Document document = new Document("_id", id)
                    .append("name", name)
                    .append("points", 0)
                    .append("wins", wins);
            insertOne(document);
        }
        else {
            int newWins = (int)result.get("wins") + wins;
            updateOne(eq("_id", id), new Document("$set", new Document("wins", newWins)));
        }
    }

    public int getPoints(TwitchUser user) {
        long id = user.getUserID();

        Document result = find(eq("_id", id)).first();
        if (result != null) {
            return (int)result.get("wins");
        }
        return 0;
    }

    public int getWins(TwitchUser user) {
        long id = user.getUserID();

        Document result = find(eq("_id", id)).first();
        if (result != null) {
            return (int)result.get("points");
        }
        return 0;
    }
}
