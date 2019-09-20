package Util.Database;

import com.gikk.twirk.types.users.TwitchUser;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Sorts;
import org.bson.Document;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import static com.mongodb.client.model.Filters.*;
import static java.lang.System.out;

public class SpeedySpinLeaderboard extends CollectionBase{
    private static SpeedySpinLeaderboard instance = null;

    private final String COLLECTION_NAME = "speedyspin";
    private final String ID_KEY = "_id";
    private final String NAME_KEY = "name";
    private final String POINTS_KEY = "points";
    private final String WINS_KEY = "wins";
    
    private SpeedySpinLeaderboard() {
        super();
    }
    
    public static SpeedySpinLeaderboard getInstance() {
        if (instance == null) {
            instance = new SpeedySpinLeaderboard();
        }
        return instance;
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

        Document result = find(eq(ID_KEY, id)).first();

        if (result == null) {
            Document document = new Document(ID_KEY, id)
                    .append(NAME_KEY, name)
                    .append(POINTS_KEY, points)
                    .append(monthlyPointsKey, points)
                    .append(WINS_KEY, 0);
            insertOne(document);
        }
        else {
            int newPoints = (int)result.get(POINTS_KEY) + points;
            int newMonthlyPoints;
            if (result.get(monthlyPointsKey) == null) {
                newMonthlyPoints = 0;
            }
            else {
                newMonthlyPoints = (int)result.get(monthlyPointsKey);
            }
            newMonthlyPoints += points;

            updateOne(eq(ID_KEY, id), new Document("$set", new Document(POINTS_KEY, newPoints)));
            updateOne(eq(ID_KEY, id), new Document("$set", new Document(monthlyPointsKey, newMonthlyPoints)));
        }
    }

    public void addWins(TwitchUser user, int wins) {
        String monthlyPoints = getMonthlyPointsKey();
        long id = user.getUserID();
        String name = user.getDisplayName();

        Document result = find(eq(ID_KEY, id)).first();

        if (result == null) {
            Document document = new Document(ID_KEY, id)
                    .append(NAME_KEY, name)
                    .append(POINTS_KEY, 0)
                    .append(monthlyPoints, 0)
                    .append(WINS_KEY, wins);
            insertOne(document);
        }
        else {
            int newWins = (int)result.get(WINS_KEY) + wins;
            updateOne(eq(ID_KEY, id), new Document("$set", new Document(WINS_KEY, newWins)));
        }
    }

    public int getPoints(TwitchUser user) {
        long id = user.getUserID();

        Document result = find(eq(ID_KEY, id)).first();
        if (result != null) {
            return (int)result.get(POINTS_KEY);
        }
        return 0;
    }

    public int getPrevMonthlyPoints(Document user) {
        Object monthlyPoints = user.get(getPrevMonthlyPointsKey());
        if (monthlyPoints != null) {
            return (int)monthlyPoints;
        }
        else {
            return 0;
        }
    }

    public int getPoints(long id) {
        Document result = find(eq(ID_KEY, id)).first();
        if (result != null) {
            return (int)result.get(POINTS_KEY);
        }
        return 0;
    }

    public int getMonthlyPoints(TwitchUser user) {
        long id = user.getUserID();

        Document result = find(eq(ID_KEY, id)).first();
        if (result != null) {
            Object monthlyPoints = result.get(getMonthlyPointsKey());
            if (monthlyPoints != null) {
                return (int)monthlyPoints;
            }
        }
        return 0;
    }

    public int getMonthlyPoints(long id) {
        Document result = find(eq(ID_KEY, id)).first();
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

        Document result = find(eq(ID_KEY, id)).first();
        if (result != null) {
            return (int)result.get(WINS_KEY);
        }
        return 0;
    }

    public String getUsername(long id) {
        Document result = find(eq(ID_KEY, id)).first();
        if (result != null) {
            return (String)result.get(NAME_KEY);
        }
        return "N/A";
    }

    public String getUsername(Document user) {
        return (String)user.get(NAME_KEY);
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
                topMonthlyScorers.add((long)next.get(ID_KEY));
            }
        }
        
        return topMonthlyScorers;
    }
    
    //returns id's of top 3 all-time scorers. if there are less than 3, returns -1 for those slots
    public ArrayList<Long> getTopScorers() {
        ArrayList<Long> topScorers = new ArrayList<>();
        
        MongoCursor<Document> result = find().sort(Sorts.descending(POINTS_KEY)).iterator();
        while (result.hasNext() && topScorers.size() < 3) {
            Document next = result.next();
            if (next.get(POINTS_KEY) == null) {
                break;
            }
            else {
                topScorers.add((long)next.get(ID_KEY));
            }
        }
        
        return topScorers;
    }

    public void logPreviousTopMonthlyScorers() {
        MongoCursor<Document> result = find().sort(Sorts.descending(getPrevMonthlyPointsKey())).iterator();
        int index = 1;
        out.println("Last Month's Top Scorers:");
        while (result.hasNext() && index < 21) {
            Document next = result.next();
            if (next.get(getPrevMonthlyPointsKey()) == null) {
                break;
            }
            else {
                String name = getUsername(next);
                int points = getPrevMonthlyPoints(next);
                out.println(String.format("%d. %s - %d", index, name, points));
                index++;
            }
        }
    }

    private String getMonthlyPointsKey() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        return String.format("points%d%d", year, month);
    }

    private String getPrevMonthlyPointsKey() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        calendar.add(Calendar.MONTH, -1);
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        return String.format("points%d%d", year, month);
    }
}
