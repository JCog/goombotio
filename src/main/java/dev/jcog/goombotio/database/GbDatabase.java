package dev.jcog.goombotio.database;

import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCredential;
import com.mongodb.MongoSecurityException;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import dev.jcog.goombotio.util.StartupException;

import java.util.Collections;

public class GbDatabase {
    private static final Logger log = LoggerFactory.getLogger(GbDatabase.class);
    private static final String ADMIN_DB = "admin";
    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = 27017;
    private final MongoClient mongoClient;
    private final MongoDatabase mongoDatabase;
    private final boolean writePermission;

    public GbDatabase(String host, int port, String dbName, String user, String password, boolean writePermission) {
        log.info("Establishing database connection to {} at {}:{}... ", dbName, host, port);
        this.writePermission = writePermission;
        ServerAddress serverAddress = new ServerAddress(host, port);
        if (user == null || password == null) {
            mongoClient = MongoClients.create(MongoClientSettings.builder()
                    .applyToClusterSettings(builder -> builder.hosts(Collections.singletonList(serverAddress)))
                    .build()
            );
        } else {
            MongoCredential credential = MongoCredential.createCredential(
                    user,
                    ADMIN_DB,
                    password.toCharArray()
            );
            mongoClient = MongoClients.create(MongoClientSettings.builder()
                    .applyToClusterSettings(builder -> builder.hosts(Collections.singletonList(serverAddress)))
                    .credential(credential)
                    .build()
            );
        }
        boolean dbExists = false;
        try {
            for (String databaseName : mongoClient.listDatabaseNames()) {
                if (databaseName.equals(dbName)) {
                    dbExists = true;
                    break;
                }
            }
        } catch (MongoSecurityException e) {
            throw new StartupException("unable to authenticate with database");
        }
        if (!dbExists) {
            throw new StartupException(String.format("database \"%s\" does not exist", dbName));
        }
        mongoDatabase = mongoClient.getDatabase(dbName);
        log.info("Database connection established.");
    }
    
    public GbDatabase(String dbName, boolean writePermission) {
        this(DEFAULT_HOST, DEFAULT_PORT, dbName, null, null, writePermission);
    }

    public void close() {
        mongoClient.close();
    }

    public MongoCollection<Document> getCollection(String collection) {
        return mongoDatabase.getCollection(collection);
    }

    public boolean hasWritePermission() {
        return writePermission;
    }
}
