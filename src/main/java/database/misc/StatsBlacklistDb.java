package database.misc;

import database.GbCollection;
import database.GbDatabase;
import org.bson.Document;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class StatsBlacklistDb extends GbCollection {
    private static final String COLLECTION_NAME = "stats_blacklist";
    
    private static final String USERNAME_KEY = "username";
    
    public StatsBlacklistDb(GbDatabase gbDatabase) {
        super(gbDatabase, COLLECTION_NAME);
    }
    
    public void addBlacklistedUser(String userId) {
        Document user = findFirstEquals(ID_KEY, userId);
        if (user == null) {
            Document document = new Document(ID_KEY, userId);
            insertOne(document);
        }
    }
    
    public void updateUsername(String userId, String username) {
        Document user = findFirstEquals(ID_KEY, userId);
        if (user == null) {
            return;
        }
        Document document = new Document(ID_KEY, userId)
                .append(USERNAME_KEY, username);
        updateOne(userId, document);
    }
    
    public boolean isBlacklisted(String userId) {
        return findFirstEquals(ID_KEY, userId) != null;
    }
    
    
    public List<String> getAllIdsList() {
        return findAll().map(document -> document.getString(ID_KEY)).into(new ArrayList<>());
    }
    
    public Set<String> getAllIdsSet() {
        return findAll().map(document -> document.getString(ID_KEY)).into(new HashSet<>());
    }
    
}
