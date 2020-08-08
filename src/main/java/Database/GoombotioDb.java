package Database;

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

    private MongoClient mongoClient;
    private MongoDatabase mongoDatabase;

    private GoombotioDb() {

    }

    public void init(String host, int port, String user, String password) {
        MongoCredential credential = MongoCredential.createCredential(
                user,
                DATABASE_NAME,
                password.toCharArray()
        );
        mongoClient = MongoClients.create(
                MongoClientSettings.builder()
                        .applyToClusterSettings(builder ->
                                builder.hosts(Collections.singletonList(
                                        new ServerAddress(host, port)
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
