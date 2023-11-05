package database.misc;

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
    
    public void editPermanentProp(String userId, boolean permanent) {
        initUser(userId);
        Document document = new Document(ID_KEY, userId)
                .append(PERMANENT_KEY, permanent);
        updateOne(userId, document);
    }
    
    public void editBlacklistProp(String userId, boolean blacklist) {
        initUser(userId);
        Document document = new Document(ID_KEY, userId)
                .append(BLACKLISTED_KEY, blacklist);
        updateOne(userId, document);
    }
    
    public void editRaffleWinnerProp(String userId, boolean raffleWinner) {
        initUser(userId);
        Document document = new Document(ID_KEY, userId)
                .append(RAFFLE_WINNER_KEY, raffleWinner);
        updateOne(userId, document);
    }
    
    public void editThroneProp(String userId, boolean throne) {
        initUser(userId);
        Document document = new Document(ID_KEY, userId)
                .append(THRONE_KEY, throne);
        updateOne(userId, document);
    }
    
    
    
    public boolean isPermanentVip(String userId) {
        return findFirstEquals(ID_KEY, userId).getBoolean(PERMANENT_KEY);
    }
    
    public boolean isBlacklisted(String userId) {
        return findFirstEquals(ID_KEY, userId).getBoolean(BLACKLISTED_KEY);
    }
    
    public boolean isRaffleWinnerVip(String userId) {
        return findFirstEquals(ID_KEY, userId).getBoolean(RAFFLE_WINNER_KEY);
    }
    
    public boolean isThroneVip(String userId) {
        return findFirstEquals(ID_KEY, userId).getBoolean(THRONE_KEY);
    }
    
    
    
    public boolean hasVip(String userId) {
        Document user = initUser(userId);
        boolean permanent = user.containsKey(PERMANENT_KEY) && user.getBoolean(PERMANENT_KEY);
        boolean raffleWinner = user.containsKey(RAFFLE_WINNER_KEY) && user.getBoolean(RAFFLE_WINNER_KEY);
        boolean throne = user.containsKey(THRONE_KEY) && user.getBoolean(THRONE_KEY);
        
        return permanent || raffleWinner || throne;
    }
    
    public String getThroneUserId() {
        return findFirstEquals(THRONE_KEY, true).getString(ID_KEY);
    }
    
    public List<String> getAllPermanentVipUserIds() {
        return findEquals(PERMANENT_KEY, true).map(document -> document.getString(ID_KEY)).into(new ArrayList<>());
    }
    
    public List<String> getAllBlacklistedUserIds() {
        return findEquals(BLACKLISTED_KEY, true).map(document -> document.getString(ID_KEY)).into(new ArrayList<>());
    }
    
    public List<String> getAllRaffleWinnerUserIds() {
        return findEquals(RAFFLE_WINNER_KEY, true).map(document -> document.getString(ID_KEY)).into(new ArrayList<>());
    }
    
    public List<String> getAllIds() {
        return findAll().map(document -> document.getString(ID_KEY)).into(new ArrayList<>());
    }
}
