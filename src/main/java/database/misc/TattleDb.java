package database.misc;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCursor;
import database.GbCollection;
import database.GbDatabase;
import database.entries.TattleItem;
import org.bson.Document;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Random;

public class TattleDb extends GbCollection {
    private static final String COLLECTION_NAME = "tattles";
    
    private static final String TATTLE_KEY = "tattle";

    private final Random random = new Random();

    public TattleDb(GbDatabase gbDatabase) {
        super(gbDatabase, COLLECTION_NAME);
    }

    public void addTattle(String twitchId, String tattle) {
        Document result = findFirstEquals(ID_KEY, twitchId);
        if (result == null) {
            Document document = new Document(ID_KEY, twitchId)
                    .append(TATTLE_KEY, tattle);
            insertOne(document);
        } else {
            updateOne(twitchId, new Document(TATTLE_KEY, tattle));
        }
    }

    @Nullable
    public TattleItem getTattle(String twitchId) {
        Document result = findFirstEquals(ID_KEY, twitchId);
        if (result == null) {
            return null;
        } else {
            return new TattleItem(result.getString(ID_KEY), result.getString(TATTLE_KEY));
        }
    }

    public TattleItem getRandomTattle() {
        int index = random.nextInt((int) countDocuments());
        MongoCursor<Document> iterator = findAll().iterator();
        for (int i = 0; i < index; i++) {
            iterator.next();
        }
        Document tattle = iterator.next();
        return new TattleItem(tattle.getString(ID_KEY), tattle.getString(TATTLE_KEY));
    }
    
    public ArrayList<TattleItem> getAllTattles() {
        FindIterable<Document> documents = findAll();
        ArrayList<TattleItem> tattles = new ArrayList<>();
        for (Document document : documents) {
            tattles.add(new TattleItem(document.getString(ID_KEY), document.getString(TATTLE_KEY)));
        }
        return tattles;
    }
}
