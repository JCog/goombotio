package Util.Database;

import com.gikk.twirk.types.users.TwitchUser;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Sorts;
import org.bson.Document;

import javax.print.Doc;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

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
        String monthlyPointsKey = getMonthlyPointsKey();
        long id = user.getUserID();
        String name = user.getDisplayName();

        Document result = find(eq("_id", id)).first();

        if (result == null) {
            Document document = new Document("_id", id)
                    .append("name", name)
                    .append("points", points)
                    .append(monthlyPointsKey, points)
                    .append("wins", 0);
            insertOne(document);
        }
        else {
            int newPoints = (int)result.get("points") + points;
            int newMonthlyPoints;
            if (result.get(monthlyPointsKey) == null) {
                newMonthlyPoints = 0;
            }
            else {
                newMonthlyPoints = (int)result.get(monthlyPointsKey);
            }
            newMonthlyPoints += points;

            updateOne(eq("_id", id), new Document("$set", new Document("points", newPoints)));
            updateOne(eq("_id", id), new Document("$set", new Document(monthlyPointsKey, newMonthlyPoints)));
        }
    }

    public void addWins(TwitchUser user, int wins) {
        String monthlyPoints = getMonthlyPointsKey();
        long id = user.getUserID();
        String name = user.getDisplayName();

        Document result = find(eq("_id", id)).first();

        if (result == null) {
            Document document = new Document("_id", id)
                    .append("name", name)
                    .append("points", 0)
                    .append(monthlyPoints, 0)
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

    public int getMonthlyPoints(TwitchUser user) {
        long id = user.getUserID();

        Document result = find(eq("_id", id)).first();
        if (result != null) {
            Object monthlyPoints = result.get(getMonthlyPointsKey());
            if (monthlyPoints != null) {
                return (int)monthlyPoints;
            }
        }
        return 0;
    }

    public int getMonthlyPoints(long id) {
        Document result = find(eq("_id", id)).first();
        if (result != null) {
            Object monthlyPoints = result.get(getMonthlyPointsKey());
            if (monthlyPoints != null) {
                return (int)monthlyPoints;
            }
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

    //returns id's of top 3 monthly scorers. if there are less than 3, returns -1 for those slots
    public ArrayList<Long> getTopMonthlyScorers() {
        ArrayList<Long> topMonthlyScorers = new ArrayList<>();

        MongoCursor<Document> result = find().sort(Sorts.descending(getMonthlyPointsKey())).iterator();
        while (result.hasNext() && topMonthlyScorers.size() < 3) {
            Document next = result.next();
            if (next.get(getMonthlyPointsKey()) == null) {
                break;
            }
            else {
                topMonthlyScorers.add((long)next.get("_id"));
            }
        }

        return topMonthlyScorers;
    }

    private String getMonthlyPointsKey() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        return String.format("points%d%d", year, month);
    }
}
