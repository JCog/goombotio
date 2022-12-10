package database.emotes;

import com.mongodb.client.MongoCursor;
import database.GbCollection;
import database.GbDatabase;
import database.entries.EmoteItem;
import org.bson.Document;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class EmoteStatsDb extends GbCollection {

    private static final String COLLECTION_NAME_KEY = "emotestats";
    private static final String EMOTE_PATTERN_KEY = "emote_pattern";
    private static final String MONTH_KEY = "month";
    private static final String USAGE_STATS_KEY = "usage_stats";
    private static final String COUNT_KEY = "count";
    private static final String USERS_KEY = "users";

    public EmoteStatsDb(GbDatabase gbDatabase) {
        super(gbDatabase);
    }

    @Override
    protected String getCollectionName() {
        return COLLECTION_NAME_KEY;
    }

    public void addEmoteUsage(String emoteId, String pattern, long userId) {
        Document result = getEmoteById(emoteId);
        String monthKeyValue = getMonthKeyValue();

        if (result == null) {
            ArrayList<Document> usageStatsList = new ArrayList<>();
            usageStatsList.add(generateNewUsageStats(userId, monthKeyValue));

            Document mainDocument = new Document(ID_KEY, emoteId)
                    .append(EMOTE_PATTERN_KEY, pattern)
                    .append(USAGE_STATS_KEY, usageStatsList);

            insertOne(mainDocument);
        } else {
            List<Document> usageStatsList = result.getList(USAGE_STATS_KEY, Document.class);
            Document currentMonthStats = getUsageStats(usageStatsList, monthKeyValue);

            //if emote exists but hasn't been used this month
            if (currentMonthStats == null) {
                Document usageStatsDocument = generateNewUsageStats(userId, monthKeyValue);
                usageStatsList.add(usageStatsDocument);

                updateOne(emoteId, new Document(USAGE_STATS_KEY, usageStatsList));
            } else {
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
                updateOne(emoteId, new Document(USAGE_STATS_KEY, usageStatsList));
            }

        }
    }

//    public void addEmoteUsage(Emote emote, long userId) {
//        addEmoteUsage(emote.getEmoteIDString(), emote.getPattern(), userId);
//    }

    public int getTotalEmoteUsageCount(String pattern) {
        Document emoteDoc = getEmoteByPattern(pattern);
        if (emoteDoc == null) {
            return 0;
        }

        int count = 0;
        List<Document> usageStatsList = emoteDoc.getList(USAGE_STATS_KEY, Document.class);
        for (Document monthDoc : usageStatsList) {
            count += monthDoc.getInteger(COUNT_KEY);
        }
        return count;
    }

    public int getMonthlyEmoteUsageCount(String pattern) {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        return getMonthlyEmoteUsageCount(pattern, year, month);
    }

    public int getMonthlyEmoteUsageCount(String pattern, int year, int month) {
        String monthKeyValue = getMonthKeyValue(year, month);
        Document emoteDoc = getEmoteByPattern(pattern);
        if (emoteDoc == null) {
            return 0;
        }

        List<Document> usageStatsList = emoteDoc.getList(USAGE_STATS_KEY, Document.class);
        for (Document monthDoc : usageStatsList) {
            if (monthDoc.getString(MONTH_KEY).equals(monthKeyValue)) {
                return monthDoc.getInteger(COUNT_KEY);
            }
        }
        return 0;
    }

    public Vector<EmoteItem> getTopMonthlyEmoteCounts(String prefix) {
        Vector<EmoteItem> topEmotes = getEmoteItems(getMonthKeyValue(), prefix);
        topEmotes.sort(new SortEmotesDescendingByCount());
        return topEmotes;
    }

    public Vector<EmoteItem> getTopMonthlyEmoteCounts() {
        return getTopMonthlyEmoteCounts(null);
    }

    public Vector<EmoteItem> getTopEmoteCounts(String prefix) {
        Vector<EmoteItem> topEmotes = getEmoteItems(null, prefix);
        topEmotes.sort(new SortEmotesDescendingByCount());
        return topEmotes;
    }

    public Vector<EmoteItem> getTopEmoteCounts() {
        return getTopEmoteCounts(null);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private Document getEmoteById(String id) {
        return findFirstEquals(ID_KEY, id);
    }

    private Document getEmoteByPattern(String pattern) {
        return findFirstEquals(EMOTE_PATTERN_KEY, pattern);
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

    @Nullable
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

    private Vector<EmoteItem> getEmoteItems(String monthKeyValue, String prefix) {
        MongoCursor<Document> result = findAll().iterator();
        Vector<EmoteItem> topEmotes = new Vector<>();

        while (result.hasNext()) {
            Document emote = result.next();
            if (prefix == null || emote.getString(EMOTE_PATTERN_KEY).startsWith(prefix)) {
                List<Document> usageStatsList = emote.getList(USAGE_STATS_KEY, Document.class);
                int count = 0;
                int users = 0;
                for (Document monthDoc : usageStatsList) {
                    if (monthKeyValue == null || monthDoc.getString(MONTH_KEY).equals(monthKeyValue)) {
                        count += monthDoc.getInteger(COUNT_KEY);
                        users += monthDoc.getList(USERS_KEY, Long.class).size();
                    }
                }
                topEmotes.add(new EmoteItem(emote.getString(EMOTE_PATTERN_KEY), count, users));
            }
        }
        return topEmotes;
    }

    private static class SortEmotesDescendingByCount implements Comparator<EmoteItem> {

        @Override
        public int compare(EmoteItem o1, EmoteItem o2) {
            return o2.getCount() - o1.getCount();
        }
    }

    private static class SortEmotesDescendingByUsers implements Comparator<EmoteItem> {

        @Override
        public int compare(EmoteItem o1, EmoteItem o2) {
            return o2.getUsers() - o1.getUsers();
        }
    }
}
