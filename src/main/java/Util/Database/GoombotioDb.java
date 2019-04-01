package Util.Database;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

public class GoombotioDb {

    private final String DATABASE_NAME = "goombotio";

    private static GoombotioDb goombotioDb = null;
    private MongoClient mongoClient;
    private MongoDatabase mongoDatabase;

    private GoombotioDb() {
        mongoClient = new MongoClient();
        mongoDatabase = mongoClient.getDatabase(DATABASE_NAME);
    }

    public static GoombotioDb getInstance() {
        if (goombotioDb == null) {
            goombotioDb = new GoombotioDb();
        }
        return goombotioDb;
    }

    public void close() {
        mongoClient.close();
    }

    public MongoCollection<Document> getCollection(String collection) {
        return mongoDatabase.getCollection(collection);
    }
}
