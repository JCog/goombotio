package database.misc;

import database.GbCollection;
import database.GbDatabase;
import org.bson.Document;

import java.util.ArrayList;

public class BitWarDb extends GbCollection {
    private static final String COLLECTION_NAME_KEY = "bit_war";
    private static final String TOTAL_KEY_BASE = "total_";
    private static final String CURRENT_KEY_BASE = "current_";

    public BitWarDb(GbDatabase gbDatabase) {
        super(gbDatabase);
    }

    @Override
    protected String getCollectionName() {
        return COLLECTION_NAME_KEY;
    }

    public void addBits(String bitWar, String team, int amount) {
        String totalKey = getTotalKey(team);
        String currentKey = getCurrentKey(team);
        Document result = findFirstEquals(ID_KEY, bitWar);
        if (result == null) {
            Document document = new Document(ID_KEY, bitWar)
                    .append(totalKey, amount)
                    .append(currentKey, amount);
            insertOne(document);
        } else if (result.get(totalKey) == null) {
            updateOne(bitWar, new Document(totalKey, amount));
            updateOne(bitWar, new Document(currentKey, amount));
        } else {
            int newTotal = (int) result.get(totalKey) + amount;
            int newCurrent = (int) result.get(currentKey) + amount;
            updateOne(bitWar, new Document(totalKey, newTotal));
            updateOne(bitWar, new Document(currentKey, newCurrent));
        }
    }

    public void resetBitWar(String bitWar, ArrayList<String> teams) {
        Document result = findFirstEquals(ID_KEY, bitWar);
        if (result != null) {
            for (String team : teams) {
                String currentKey = getCurrentKey(team);
                if (result.get(currentKey) != null) {
                    updateOne(bitWar, new Document(currentKey, 0));
                }
            }
        }
    }

    public int getCurrentAmount(String bitWar, String team) {
        String currentKey = getCurrentKey(team);
        Document result = findFirstEquals(ID_KEY, bitWar);
        if (result != null && result.get(currentKey) != null) {
            return (int) result.get(currentKey);
        }
        return 0;
    }

    public int getTotalAmount(String bitWar, String team) {
        String totalKey = getTotalKey(team);
        Document result = findFirstEquals(ID_KEY, bitWar);
        if (result != null && result.get(totalKey) != null) {
            return (int) result.get(totalKey);
        }
        return 0;
    }

    private String getTotalKey(String team) {
        return TOTAL_KEY_BASE + team.toLowerCase();
    }

    private String getCurrentKey(String team) {
        return CURRENT_KEY_BASE + team.toLowerCase();
    }
}
