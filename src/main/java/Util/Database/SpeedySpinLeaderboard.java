package Util.Database;

import com.gikk.twirk.types.users.TwitchUser;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import javafx.util.Pair;
import org.bson.Document;

import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Updates.combine;
import static com.mongodb.client.model.Updates.set;

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
        long id = user.getUserID();
        String name = user.getDisplayName();

        Document result = find(eq("_id", id)).first();

        if (result == null) {
            Document document = new Document("_id", id)
                    .append("name", name)
                    .append("points", points)
                    .append("wins", wins);
            insertOne(document);
        }
        else {
            //TODO: figure out how to actually increment instead of replacing the whole document
            updateOne(eq("_id", id),
                    combine(set("name", name),
                            set("points", points + (int)result.get("points")),
                            set("wins", wins + (int)result.get("wins"))));
        }
    }

    public Pair<Integer, Integer> getPointsAndWins(TwitchUser user) {
        long id = user.getUserID();

        Document result = find(eq("_id", id)).first();
        if (result != null) {
            return new Pair<>((int)result.get("points"), (int)result.get("wins"));
        }

        return null;
    }
}
