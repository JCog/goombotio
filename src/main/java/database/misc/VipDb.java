package database.misc;

import com.mongodb.lang.Nullable;
import database.GbCollection;
import database.GbDatabase;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;

public class VipDb extends GbCollection {
    private static final String COLLECTION_NAME = "vips";
    
    private static final String USERNAME_KEY = "username";
    private static final String PERMANENT_KEY = "permanent";
    private static final String BLACKLISTED_KEY = "blacklist";
    private static final String RAFFLE_WINNER_KEY = "raffle_winner";
    private static final String THRONE_KEY = "throne";
    
    public VipDb(GbDatabase gbDatabase) {
        super(gbDatabase, COLLECTION_NAME);
    }
    
    private Document initUser(String userId) {
        Document user = findFirstEquals(ID_KEY, userId);
        if (user == null) {
            Document document = new Document(ID_KEY, userId);
            insertOne(document);
        }
        return user;
    }
    
    
    
    public void updateUsername(String userId, String username) {
        initUser(userId);
        Document document = new Document(ID_KEY, userId)
                .append(USERNAME_KEY, username);
        updateOne(userId, document);
    }
    
    private void updateProperty(String userId, boolean property, String propertyKey) {
        initUser(userId);
        Document document = new Document(ID_KEY, userId)
                .append(propertyKey, property);
        updateOne(userId, document);
    }
    
    public void editPermanentProp(String userId, boolean permanent) {
        updateProperty(userId, permanent, PERMANENT_KEY);
    }
    
    public void editBlacklistProp(String userId, boolean blacklist) {
        updateProperty(userId, blacklist, BLACKLISTED_KEY);
    }
    
    public void editRaffleWinnerProp(String userId, boolean raffleWinner) {
        updateProperty(userId, raffleWinner, RAFFLE_WINNER_KEY);
    }
    
    public void editThroneProp(String userId, boolean throne) {
        updateProperty(userId, throne, THRONE_KEY);
    }
    
    
    
    private boolean isProperty(String userId, String propertyKey) {
        Document document = findFirstEquals(ID_KEY, userId);
        if (document == null) {
            return false;
        }
        Boolean result = document.getBoolean(propertyKey);
        if (document.getBoolean(propertyKey) == null) {
            return false;
        }
        return result;
    }
    
    public boolean isPermanentVip(String userId) {
        return isProperty(userId, PERMANENT_KEY);
    }
    
    public boolean isBlacklisted(String userId) {
        return isProperty(userId, BLACKLISTED_KEY);
    }
    
    public boolean isRaffleWinnerVip(String userId) {
        return isProperty(userId, RAFFLE_WINNER_KEY);
    }
    
    public boolean isThroneVip(String userId) {
        return isProperty(userId, THRONE_KEY);
    }
    
    
    
    public boolean hasVip(String userId) {
        Document user = findFirstEquals(ID_KEY, userId);
        if (user == null) {
            return false;
        }
        boolean permanent = user.containsKey(PERMANENT_KEY) && user.getBoolean(PERMANENT_KEY);
        boolean raffleWinner = user.containsKey(RAFFLE_WINNER_KEY) && user.getBoolean(RAFFLE_WINNER_KEY);
        boolean throne = user.containsKey(THRONE_KEY) && user.getBoolean(THRONE_KEY);
        
        return permanent || raffleWinner || throne;
    }
    
    public @Nullable String getThroneUserId() {
        Document document = findFirstEquals(THRONE_KEY, true);
        if (document == null) {
            return null;
        }
        return document.getString(ID_KEY);
    }
    
    private List<String> getAllWithProp(String propertyKey) {
        return findEquals(propertyKey, true).map(document -> document.getString(ID_KEY)).into(new ArrayList<>());
    }
    
    public List<String> getAllPermanentVipUserIds() {
        return getAllWithProp(PERMANENT_KEY);
    }
    
    public List<String> getAllBlacklistedUserIds() {
        return getAllWithProp(BLACKLISTED_KEY);
    }
    
    public List<String> getAllRaffleWinnerUserIds() {
        return getAllWithProp(RAFFLE_WINNER_KEY);
    }
    
    public List<String> getAllIds() {
        return findAll().map(document -> document.getString(ID_KEY)).into(new ArrayList<>());
    }
}
