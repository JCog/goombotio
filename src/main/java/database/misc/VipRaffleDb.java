package database.misc;

import com.mongodb.client.FindIterable;
import database.GbCollection;
import database.GbDatabase;
import org.bson.Document;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class VipRaffleDb extends GbCollection {
    private static final String COLLECTION_NAME = "vip_raffle";
    
    public VipRaffleDb(GbDatabase gbDatabase) {
        super(gbDatabase, COLLECTION_NAME);
    }
    
    public void incrementEntryCount(String twitchId, int count) {
        Calendar calendar = Calendar.getInstance();
        incrementEntryCount(twitchId, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), count);
    }
    
    public void incrementEntryCount(String twitchId, int year, int month, int count) {
        Document result = getVipRaffleDocument(twitchId);
        String monthlyEntryKey = getMonthlyEntriesKey(year, month);
        if (result == null) {
            Document document = new Document(ID_KEY, twitchId)
                    .append(monthlyEntryKey, count);
            insertOne(document);
        } else {
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
        FindIterable<Document> documents = findAll();
        List<VipRaffleItem> vipRaffleItems = new ArrayList<>();
        String monthlyEntriesKey = getMonthlyEntriesKey(year, month);
        for (Document document : documents) {
            if (document.containsKey(monthlyEntriesKey)) {
                vipRaffleItems.add(new VipRaffleItem(document.getString(ID_KEY), document.getInteger(monthlyEntriesKey)));
            }
        }
        return vipRaffleItems;
    }
    
    @Nullable
    public VipRaffleItem getVipRaffleItem(String twitchId) {
        Document result = getVipRaffleDocument(twitchId);
        if (result != null) {
            return new VipRaffleItem(result.getString(ID_KEY), result.getInteger(getMonthlyEntriesKey()));
        }
        return null;
    }
    
    @Nullable
    public VipRaffleItem getVipRaffleItem(String twitchId, int year, int month) {
        Document result = getVipRaffleDocument(twitchId);
        if (result != null) {
            return new VipRaffleItem(result.getString(ID_KEY), result.getInteger(getMonthlyEntriesKey(year, month)));
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
        private final int entryCount;
        
        public VipRaffleItem(String twitchId, int entryCount) {
            this.twitchId = twitchId;
            this.entryCount = entryCount;
        }
        
        public String getTwitchId() {
            return twitchId;
        }
        
        public int getEntryCount() {
            return entryCount;
        }
    }
}
