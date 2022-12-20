package database.preds;

import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Sorts;
import database.GbCollection;
import database.GbDatabase;
import org.bson.Document;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

public abstract class PredsLeaderboardDb extends GbCollection {
    static final String NAME_KEY = "name";
    static final String POINTS_KEY = "points";
    static final String WINS_KEY = "wins";

    protected PredsLeaderboardDb(GbDatabase gbDatabase, String collectionName) {
        super(gbDatabase, collectionName);
    }

    public void addPointsAndWins(String userId, String displayName, int points, int wins) {
        addPoints(userId, displayName, points);
        addWins(userId, displayName, wins);
    }

    public void addPoints(String userId, String displayName, int points) {
        String monthlyPointsKey = getMonthlyPointsKey();
        long id = Long.parseLong(userId);

        Document result = findFirstEquals(ID_KEY, id);

        if (result == null) {
            Document document = new Document(ID_KEY, id)
                    .append(NAME_KEY, displayName)
                    .append(POINTS_KEY, points)
                    .append(monthlyPointsKey, points)
                    .append(WINS_KEY, 0);
            insertOne(document);
        } else {
            int newPoints = result.getInteger(POINTS_KEY) + points;
            int newMonthlyPoints;
            if (result.get(monthlyPointsKey) == null) {
                newMonthlyPoints = 0;
            } else {
                newMonthlyPoints = result.getInteger(monthlyPointsKey);
            }
            newMonthlyPoints += points;

            updateOne(id, new Document(NAME_KEY, displayName));
            updateOne(id, new Document(POINTS_KEY, newPoints));
            updateOne(id, new Document(monthlyPointsKey, newMonthlyPoints));
        }
    }

    public void addWins(String userId, String displayName, int wins) {
        String monthlyPoints = getMonthlyPointsKey();
        long id = Long.parseLong(userId);

        Document result = findFirstEquals(ID_KEY, id);

        if (result == null) {
            Document document = new Document(ID_KEY, id)
                    .append(NAME_KEY, displayName)
                    .append(POINTS_KEY, 0)
                    .append(monthlyPoints, 0)
                    .append(WINS_KEY, wins);
            insertOne(document);
        } else {
            int newWins = result.getInteger(WINS_KEY) + wins;
            updateOne(id, new Document(WINS_KEY, newWins));
        }
    }

    public int getPrevMonthlyPoints(Document user) {
        return user.getInteger(getPrevMonthlyPointsKey());
    }

    public int getPoints(long userId) {
        Document result = findFirstEquals(ID_KEY, userId);
        if (result != null) {
            return result.getInteger(POINTS_KEY);
        }
        return 0;
    }
    
    public int getPoints(String userId) {
        return getPoints(Long.parseLong(userId));
    }

    public int getMonthlyPoints(long userId) {
        Document result = findFirstEquals(ID_KEY, userId);
        if (result != null) {
            Integer monthlyPoints = result.getInteger(getMonthlyPointsKey());
            if (monthlyPoints != null) {
                return monthlyPoints;
            }
        }
        return 0;
    }

    public int getWins(long userId) {
        Document result = findFirstEquals(ID_KEY, userId);
        if (result != null) {
            return result.getInteger(WINS_KEY);
        }
        return 0;
    }

    public String getUsername(long userId) {
        Document result = findFirstEquals(ID_KEY, userId);
        if (result != null) {
            return result.getString(NAME_KEY);
        }
        return "N/A";
    }

    public String getUsername(Document user) {
        return user.getString(NAME_KEY);
    }

    //returns IDs of top monthly scorers
    public ArrayList<Long> getTopMonthlyScorers() {
        ArrayList<Long> topMonthlyScorers = new ArrayList<>();

        for (Document next : findAll().sort(Sorts.descending(getMonthlyPointsKey()))) {
            if (next.get(getMonthlyPointsKey()) == null) {
                break;
            } else {
                topMonthlyScorers.add(next.getLong(ID_KEY));
            }
        }

        return topMonthlyScorers;
    }

    //returns id's of top all-time scorers, up to the number of results specified by limit
    public ArrayList<Long> getTopScorers(Integer limit) {
        ArrayList<Long> topScorers = new ArrayList<>();

        MongoCursor<Document> result = findAll().sort(Sorts.descending(POINTS_KEY)).iterator();
        while (result.hasNext() && (limit == null || topScorers.size() < limit)) {
            Document next = result.next();
            if (next.get(POINTS_KEY) == null) {
                break;
            } else {
                topScorers.add(next.getLong(ID_KEY));
            }
        }

        return topScorers;
    }

    //returns id's of top all-time scorers
    public ArrayList<Long> getTopScorers() {
        return getTopScorers(null);
    }

    //returns id's of top winners
    public ArrayList<Long> getTopWinners() {
        ArrayList<Long> topScorers = new ArrayList<>();

        for (Document next : findAll().sort(Sorts.descending(WINS_KEY))) {
            if (next.get(WINS_KEY) == null) {
                break;
            } else {
                topScorers.add(next.getLong(ID_KEY));
            }
        }

        return topScorers;
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
