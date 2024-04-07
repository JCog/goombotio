package database.preds;

import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Sorts;
import database.GbCollection;
import database.GbDatabase;
import org.bson.Document;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public abstract class PredsLeaderboardDbBase extends GbCollection {
    static final String NAME_KEY = "name";
    static final String POINTS_KEY = "points";
    static final String WINS_KEY = "wins";
    
    public record PredsItem(String userId, String displayName, int wins, int points) {}

    protected PredsLeaderboardDbBase(GbDatabase gbDatabase, String collectionName) {
        super(gbDatabase, collectionName);
    }

    public void addPoints(String userId, String displayName, int points) {
        String monthlyPointsKey = getMonthlyPointsKey();

        Document result = findFirstEquals(ID_KEY, userId);

        if (result == null) {
            Document document = new Document(ID_KEY, userId)
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

            updateOne(userId, new Document(NAME_KEY, displayName));
            updateOne(userId, new Document(POINTS_KEY, newPoints));
            updateOne(userId, new Document(monthlyPointsKey, newMonthlyPoints));
        }
    }

    public void addWin(String userId, String displayName) {
        String monthlyPoints = getMonthlyPointsKey();

        Document result = findFirstEquals(ID_KEY, userId);

        if (result == null) {
            Document document = new Document(ID_KEY, userId)
                    .append(NAME_KEY, displayName)
                    .append(POINTS_KEY, 0)
                    .append(monthlyPoints, 0)
                    .append(WINS_KEY, 1);
            insertOne(document);
        } else {
            int newWins = result.getInteger(WINS_KEY) + 1;
            updateOne(userId, new Document(WINS_KEY, newWins));
        }
    }

    public int getPrevMonthlyPoints(Document user) {
        return user.getInteger(getPrevMonthlyPointsKey());
    }

    public int getPoints(String userId) {
        Document result = findFirstEquals(ID_KEY, userId);
        if (result != null) {
            return result.getInteger(POINTS_KEY);
        }
        return 0;
    }

    public int getMonthlyPoints(String userId) {
        Document result = findFirstEquals(ID_KEY, userId);
        if (result != null) {
            Integer monthlyPoints = result.getInteger(getMonthlyPointsKey());
            if (monthlyPoints != null) {
                return monthlyPoints;
            }
        }
        return 0;
    }

    public int getWins(String userId) {
        Document result = findFirstEquals(ID_KEY, userId);
        if (result != null) {
            return result.getInteger(WINS_KEY);
        }
        return 0;
    }

    public String getUsername(String userId) {
        Document result = findFirstEquals(ID_KEY, userId);
        if (result != null) {
            return result.getString(NAME_KEY);
        }
        return "N/A";
    }

    //returns IDs of top monthly scorers
    public List<String> getTopMonthlyScorers() {
        List<String> topMonthlyScorers = new ArrayList<>();

        for (Document next : findAll().sort(Sorts.descending(getMonthlyPointsKey()))) {
            if (next.get(getMonthlyPointsKey()) == null) {
                break;
            } else {
                topMonthlyScorers.add(next.getString(ID_KEY));
            }
        }

        return topMonthlyScorers;
    }

    //returns id's of top all-time scorers, up to the number of results specified by limit
    public List<String> getTopScorers(Integer limit) {
        List<String> topScorers = new ArrayList<>();

        MongoCursor<Document> result = findAll().sort(Sorts.descending(POINTS_KEY)).iterator();
        while (result.hasNext() && (limit == null || topScorers.size() < limit)) {
            Document next = result.next();
            if (next.get(POINTS_KEY) == null) {
                break;
            } else {
                topScorers.add(next.getString(ID_KEY));
            }
        }

        return topScorers;
    }

    //returns id's of top all-time scorers
    public List<String> getTopScorers() {
        return getTopScorers(null);
    }

    //returns id's of top winners
    public List<String> getTopWinners() {
        List<String> topScorers = new ArrayList<>();

        for (Document next : findAll().sort(Sorts.descending(WINS_KEY))) {
            if (next.get(WINS_KEY) == null) {
                break;
            } else {
                topScorers.add(next.getString(ID_KEY));
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
    
    public List<PredsItem> getAllSortedWins() {
        List<PredsItem> items = new ArrayList<>();
        for (Document doc : findAll().sort(Sorts.descending(WINS_KEY))) {
            if (doc.getInteger(WINS_KEY) > 0) {
                items.add(new PredsItem(
                        doc.getString(ID_KEY),
                        doc.getString(NAME_KEY),
                        doc.getInteger(WINS_KEY),
                        doc.getInteger(POINTS_KEY)
                ));
            }
        }
        return items;
    }
    
    public List<PredsItem> getAllSortedPoints() {
        List<PredsItem> items = new ArrayList<>();
        for (Document doc : findAll().sort(Sorts.descending(POINTS_KEY))) {
            items.add(new PredsItem(
                    doc.getString(ID_KEY),
                    doc.getString(NAME_KEY),
                    doc.getInteger(WINS_KEY),
                    doc.getInteger(POINTS_KEY)
            ));
        }
        return items;
    }
}
