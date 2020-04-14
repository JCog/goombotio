package Util.Database;

import com.gikk.twirk.types.emote.Emote;
import com.mongodb.client.MongoCollection;
import org.bson.Document;

import java.util.*;

import static com.mongodb.client.model.Filters.eq;

public class EmoteStatsDb extends CollectionBase {
    
    private static final String COLLECTION_NAME_KEY = "emotestats";
    private static final String ID_KEY = "_id";
    private static final String EMOTE_PATTERN_KEY = "emote_pattern";
    private static final String MONTH_KEY = "month";
    private static final String USAGE_STATS_KEY = "usage_stats";
    private static final String COUNT_KEY = "count";
    private static final String USERS_KEY = "users";
    
    private static EmoteStatsDb instance = null;
    
    private EmoteStatsDb() {
        super();
    }
    
    public static EmoteStatsDb getInstance() {
        if (instance == null) {
            instance = new EmoteStatsDb();
        }
        return instance;
    }
    
    @Override
    protected MongoCollection<Document> setCollection() {
        return goombotioDb.getCollection(COLLECTION_NAME_KEY);
    }
    
    public void addEmoteUsage(String emoteId, String pattern, long userId) {
        Document result = getEmote(emoteId);
        String monthKeyValue = getMonthKeyValue();
        
        if (result == null) {
            ArrayList<Document> usageStatsList = new ArrayList<>();
            usageStatsList.add(generateNewUsageStats(userId, monthKeyValue));
            
            Document mainDocument = new Document(ID_KEY, emoteId)
                    .append(EMOTE_PATTERN_KEY, pattern)
                    .append(USAGE_STATS_KEY, usageStatsList);
            
            insertOne(mainDocument);
        }
        else {
            List<Document> usageStatsList = result.getList(USAGE_STATS_KEY, Document.class);
            Document currentMonthStats = getUsageStats(usageStatsList, monthKeyValue);
            
            //if emote exists but hasn't been used this month
            if (currentMonthStats == null) {
                Document usageStatsDocument = generateNewUsageStats(userId, monthKeyValue);
                usageStatsList.add(usageStatsDocument);
                
                updateOne(eq(ID_KEY, emoteId), new Document("$set", new Document(USAGE_STATS_KEY, usageStatsList)));
            }
            else {
                Document newCurrentMonthStats = new Document(currentMonthStats);
                usageStatsList.remove(currentMonthStats);
                int newCount = newCurrentMonthStats.getInteger(COUNT_KEY) + 1;
                newCurrentMonthStats.put(COUNT_KEY, newCount);
                
                List<Long> usersList = newCurrentMonthStats.getList(USERS_KEY, Long.class);
                if (!usersList.contains(userId)) {
                    usersList.add(userId);
                    newCurrentMonthStats.put(USERS_KEY, usersList);
                }
                
                usageStatsList.add(newCurrentMonthStats);
                updateOne(eq(ID_KEY, emoteId), new Document("$set", new Document(USAGE_STATS_KEY, usageStatsList)));
            }
            
        }
    }
    
    public void addEmoteUsage(Emote emote, long userId) {
        addEmoteUsage(emote.getEmoteIDString(), emote.getPattern(), userId);
    }
    
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    
    private Document getEmote(String id) {
        return find(eq(ID_KEY, id)).first();
    }
    
    private String getMonthKeyValue() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        return String.format("%d-%d", year, month);
    }
    
    private String getMonthKeyValue(int year, int month) {
        return String.format("%d-%d", year, month);
    }
    
    private Document getUsageStats(List<Document> usageStats, String monthKeyValue) {
        for (Document document : usageStats) {
            if (document.getString(MONTH_KEY).equals(monthKeyValue)) {
                return document;
            }
        }
        return null;
    }
    
    private Document generateNewUsageStats(long userId, String monthKeyValue) {
        Set<Long> users = new HashSet<>();
        users.add(userId);
        return new Document(MONTH_KEY, monthKeyValue)
                .append(COUNT_KEY, 1)
                .append(USERS_KEY, users);
    }
}
