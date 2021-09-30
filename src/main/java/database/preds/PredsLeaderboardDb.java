package database.preds;

import com.gikk.twirk.types.users.TwitchUser;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Sorts;
import database.GbCollection;
import database.GbDatabase;
import org.bson.Document;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

public abstract class PredsLeaderboardDb extends GbCollection {

    private static final String NAME_KEY = "name";
    private static final String POINTS_KEY = "points";
    private static final String WINS_KEY = "wins";

    protected PredsLeaderboardDb(GbDatabase gbDatabase) {
        super(gbDatabase);
    }

    //should never actually be called
    @Override
    protected String getCollectionName() {
        return null;
    }

    public void addPointsAndWins(TwitchUser user, int points, int wins) {
        addPoints(user, points);
        addWins(user, wins);
    }

    public void addPoints(TwitchUser user, int points) {
        String monthlyPointsKey = getMonthlyPointsKey();
        long id = user.getUserID();
        String name = user.getDisplayName();

        Document result = findFirstEquals(ID_KEY, id);

        if (result == null) {
            Document document = new Document(ID_KEY, id)
                    .append(NAME_KEY, name)
                    .append(POINTS_KEY, points)
                    .append(monthlyPointsKey, points)
                    .append(WINS_KEY, 0);
            insertOne(document);
        }
        else {
            int newPoints = (int) result.get(POINTS_KEY) + points;
            int newMonthlyPoints;
            if (result.get(monthlyPointsKey) == null) {
                newMonthlyPoints = 0;
            }
            else {
                newMonthlyPoints = (int) result.get(monthlyPointsKey);
            }
            newMonthlyPoints += points;

            updateOne(id, new Document(NAME_KEY, name));
            updateOne(id, new Document(POINTS_KEY, newPoints));
            updateOne(id, new Document(monthlyPointsKey, newMonthlyPoints));
        }
    }

    public void addWins(TwitchUser user, int wins) {
        String monthlyPoints = getMonthlyPointsKey();
        long id = user.getUserID();
        String name = user.getDisplayName();

        Document result = findFirstEquals(ID_KEY, id);

        if (result == null) {
            Document document = new Document(ID_KEY, id)
                    .append(NAME_KEY, name)
                    .append(POINTS_KEY, 0)
                    .append(monthlyPoints, 0)
                    .append(WINS_KEY, wins);
            insertOne(document);
        }
        else {
            int newWins = (int) result.get(WINS_KEY) + wins;
            updateOne(id, new Document(WINS_KEY, newWins));
        }
    }

    public int getPoints(TwitchUser user) {
        long id = user.getUserID();

        Document result = findFirstEquals(ID_KEY, id);
        if (result != null) {
            return (int) result.get(POINTS_KEY);
        }
        return 0;
    }

    public int getPrevMonthlyPoints(Document user) {
        Object monthlyPoints = user.get(getPrevMonthlyPointsKey());
        if (monthlyPoints != null) {
            return (int) monthlyPoints;
        }
        else {
            return 0;
        }
    }

    public int getPoints(long id) {
        Document result = findFirstEquals(ID_KEY, id);
        if (result != null) {
            return (int) result.get(POINTS_KEY);
        }
        return 0;
    }

    public int getMonthlyPoints(TwitchUser user) {
        long id = user.getUserID();

        Document result = findFirstEquals(ID_KEY, id);
        if (result != null) {
            Object monthlyPoints = result.get(getMonthlyPointsKey());
            if (monthlyPoints != null) {
                return (int) monthlyPoints;
            }
        }
        return 0;
    }

    public int getMonthlyPoints(long id) {
        Document result = findFirstEquals(ID_KEY, id);
        if (result != null) {
            Object monthlyPoints = result.get(getMonthlyPointsKey());
            if (monthlyPoints != null) {
                return (int) monthlyPoints;
            }
        }
        return 0;
    }

    public int getWins(TwitchUser user) {
        long id = user.getUserID();

        Document result = findFirstEquals(ID_KEY, id);
        if (result != null) {
            return (int) result.get(WINS_KEY);
        }
        return 0;
    }

    public int getWins(long id) {
        Document result = findFirstEquals(ID_KEY, id);
        if (result != null) {
            return (int) result.get(WINS_KEY);
        }
        return 0;
    }

    public String getUsername(long id) {
        Document result = findFirstEquals(ID_KEY, id);
        if (result != null) {
            return (String) result.get(NAME_KEY);
        }
        return "N/A";
    }

    public String getUsername(Document user) {
        return (String) user.get(NAME_KEY);
    }

    //returns IDs of top monthly scorers
    public ArrayList<Long> getTopMonthlyScorers() {
        ArrayList<Long> topMonthlyScorers = new ArrayList<>();

        for (Document next : findAll().sort(Sorts.descending(getMonthlyPointsKey()))) {
            if (next.get(getMonthlyPointsKey()) == null) {
                break;
            }
            else {
                topMonthlyScorers.add((long) next.get(ID_KEY));
            }
        }

        return topMonthlyScorers;
    }

    //returns id's of top 3 all-time scorers. if there are less than 3, returns -1 for those slots
    public ArrayList<Long> getTopThreeScorers() {
        ArrayList<Long> topScorers = new ArrayList<>();

        MongoCursor<Document> result = findAll().sort(Sorts.descending(POINTS_KEY)).iterator();
        while (result.hasNext() && topScorers.size() < 3) {
            Document next = result.next();
            if (next.get(POINTS_KEY) == null) {
                break;
            }
            else {
                topScorers.add((long) next.get(ID_KEY));
            }
        }

        return topScorers;
    }

    //returns id's of top all-time scorers
    public ArrayList<Long> getTopScorers() {
        ArrayList<Long> topScorers = new ArrayList<>();

        for (Document next : findAll().sort(Sorts.descending(POINTS_KEY))) {
            if (next.get(POINTS_KEY) == null) {
                break;
            }
            else {
                topScorers.add((long) next.get(ID_KEY));
            }
        }

        return topScorers;
    }

    //returns id's of top winners
    public ArrayList<Long> getTopWinners() {
        ArrayList<Long> topScorers = new ArrayList<>();

        for (Document next : findAll().sort(Sorts.descending(WINS_KEY))) {
            if (next.get(WINS_KEY) == null) {
                break;
            }
            else {
                topScorers.add((long) next.get(ID_KEY));
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
