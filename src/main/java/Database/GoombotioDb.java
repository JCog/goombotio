package Database;

import Util.Settings;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

import java.util.Collections;

public class GoombotioDb {

    private static final String DATABASE_NAME = "goombotio";

    private static GoombotioDb goombotioDb = null;
    
    private final MongoClient mongoClient;
    private final MongoDatabase mongoDatabase;

    private GoombotioDb() {
        MongoCredential credential = MongoCredential.createCredential(
                Settings.getDbUser(),
                DATABASE_NAME,
                Settings.getDbPassword().toCharArray()
        );
        mongoClient = MongoClients.create(
                MongoClientSettings.builder()
                        .applyToClusterSettings(builder ->
                                builder.hosts(Collections.singletonList(
                                        new ServerAddress(Settings.getDbHost(), Settings.getDbPort())
                                )))
                        .credential(credential)
                        .build()
        );
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
