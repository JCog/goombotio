package Util.Database;

import com.gikk.twirk.types.users.TwitchUser;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Sorts;
import org.bson.Document;

import java.util.ArrayList;

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
            return (int)result.get("points");
        }
        return 0;
    }

    public int getPoints(long id) {
        Document result = find(eq("_id", id)).first();
        if (result != null) {
            return (int)result.get("points");
        }
        return 0;
    }

    public int getWins(TwitchUser user) {
        long id = user.getUserID();

        Document result = find(eq("_id", id)).first();
        if (result != null) {
            return (int)result.get("wins");
        }
        return 0;
    }

    public String getUsername(long id) {
        Document result = find(eq("_id", id)).first();
        if (result != null) {
            return (String)result.get("name");
        }
        return "N/A";
    }

    //returns id's of top 3 scorers. if there are less than 3, returns -1 for those slots
    public ArrayList<Long> getTopScorers() {
        ArrayList<Long> topScorers = new ArrayList<>();

        MongoCursor<Document> result = find().sort(Sorts.descending("points")).iterator();
        while (result.hasNext() && topScorers.size() < 3) {
            topScorers.add((long)result.next().get("_id"));
        }

        while (topScorers.size() < 3) {
            topScorers.add(-1L);
        }

        return topScorers;
    }
}
