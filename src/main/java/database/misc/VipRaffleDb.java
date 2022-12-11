package database.misc;

import com.mongodb.client.FindIterable;
import database.GbCollection;
import database.GbDatabase;
import database.entries.VipRaffleItem;
import org.bson.Document;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Calendar;

public class VipRaffleDb extends GbCollection {
    private static final String COLLECTION_NAME_KEY = "vip_raffle";
    
    public VipRaffleDb(GbDatabase gbDatabase) {
        super(gbDatabase);
    }
    
    @Override
    protected String getCollectionName() {
        return COLLECTION_NAME_KEY;
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
    
    public ArrayList<VipRaffleItem> getAllVipRaffleItemsPrevMonth() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR) - 1;
        int month = calendar.get(Calendar.MONTH);
        month = (month == Calendar.JANUARY) ? Calendar.DECEMBER : month - 1;
    
        return getAllVipRaffleItems(year, month);
    }
    
    public ArrayList<VipRaffleItem> getAllVipRaffleItems(int year, int month) {
        FindIterable<Document> documents = findAll();
        ArrayList<VipRaffleItem> vipRaffleItems = new ArrayList<>();
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
}
