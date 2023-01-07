package database.misc;

import com.mongodb.client.FindIterable;
import com.mongodb.client.model.Sorts;
import database.GbCollection;
import database.GbDatabase;
import org.bson.Document;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class VipRaffleDb extends GbCollection {
    private static final String COLLECTION_NAME = "vip_raffle";
    
    private static final String NAME_KEY = "display_name";
    
    public VipRaffleDb(GbDatabase gbDatabase) {
        super(gbDatabase, COLLECTION_NAME);
    }
    
    public void incrementEntryCount(String twitchId, String displayName, int count) {
        Calendar calendar = Calendar.getInstance();
        incrementEntryCount(twitchId, displayName, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), count);
    }
    
    public void incrementEntryCount(String twitchId, String displayName, int year, int month, int count) {
        Document result = getVipRaffleDocument(twitchId);
        String monthlyEntryKey = getMonthlyEntriesKey(year, month);
        if (result == null) {
            Document document = new Document(ID_KEY, twitchId)
                    .append(NAME_KEY, displayName)
                    .append(monthlyEntryKey, count);
            insertOne(document);
        } else {
            updateOne(twitchId, new Document(NAME_KEY, displayName));
            int newMonthlyEntryCount = count;
            if (result.containsKey(monthlyEntryKey)) {
                newMonthlyEntryCount += result.getInteger(monthlyEntryKey);
            }
            updateOne(twitchId, new Document(monthlyEntryKey, newMonthlyEntryCount));
        }
    }
    
    public List<VipRaffleItem> getAllVipRaffleItemsCurrentMonth() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        
        return getAllVipRaffleItems(year, month);
    }
    
    public List<VipRaffleItem> getAllVipRaffleItemsPrevMonth() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR) - 1;
        int month = calendar.get(Calendar.MONTH);
        month = (month == Calendar.JANUARY) ? Calendar.DECEMBER : month - 1;
        
        return getAllVipRaffleItems(year, month);
    }
    
    public List<VipRaffleItem> getAllVipRaffleItems(int year, int month) {
        String monthlyEntriesKey = getMonthlyEntriesKey(year, month);
        FindIterable<Document> documents = findAll().sort(Sorts.descending(monthlyEntriesKey));
        List<VipRaffleItem> vipRaffleItems = new ArrayList<>();
        for (Document document : documents) {
            if (document.containsKey(monthlyEntriesKey)) {
                vipRaffleItems.add(new VipRaffleItem(
                        document.getString(ID_KEY),
                        document.getString(NAME_KEY),
                        document.getInteger(monthlyEntriesKey)
                ));
            }
        }
        return vipRaffleItems;
    }
    
    @Nullable
    public VipRaffleItem getVipRaffleItem(String twitchId) {
        Document result = getVipRaffleDocument(twitchId);
        if (result != null) {
            return new VipRaffleItem(
                    result.getString(ID_KEY),
                    result.getString(NAME_KEY),
                    result.getInteger(getMonthlyEntriesKey())
            );
        }
        return null;
    }
    
    @Nullable
    public VipRaffleItem getVipRaffleItem(String twitchId, int year, int month) {
        Document result = getVipRaffleDocument(twitchId);
        if (result != null) {
            return new VipRaffleItem(
                    result.getString(ID_KEY),
                    result.getString(NAME_KEY),
                    result.getInteger(getMonthlyEntriesKey(year, month))
            );
        }
        return null;
    }
    
    private Document getVipRaffleDocument(String twitchId) {
        return findFirstEquals(ID_KEY, twitchId);
    }
    
    private String getMonthlyEntriesKey() {
        Calendar calendar = Calendar.getInstance();
        return getMonthlyEntriesKey(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH));
    }
    
    private String getMonthlyEntriesKey(int year, int month) {
        return String.format("entry_count%d-%d", year, month);
    }
    
    public static class VipRaffleItem {
        private final String twitchId;
        private final String displayName;
        private final int entryCount;
        
        public VipRaffleItem(String twitchId, String displayName, int entryCount) {
            this.twitchId = twitchId;
            this.displayName = displayName;
            this.entryCount = entryCount;
        }
        
        public String getTwitchId() {
            return twitchId;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    
        public int getEntryCount() {
            return entryCount;
        }
    }
}
