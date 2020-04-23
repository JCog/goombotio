package Util.Database;

import Util.Database.Entries.QuoteItem;
import com.mongodb.client.MongoCollection;
import org.bson.Document;

import java.util.Date;

public class QuoteDb extends CollectionBase {
    
    private static final String COLLECTION_NAME_KEY = "quotes";
    private static final String TEXT_KEY = "text";
    private static final String CREATOR_ID_KEY = "creator_id";
    private static final String CREATED_KEY = "created";
    private static final String APPROVED_KEY = "approved";
    
    private static QuoteDb instance = null;
    
    QuoteDb() {
        super();
    }
    
    public static QuoteDb getInstance() {
        if (instance == null) {
            instance = new QuoteDb();
        }
        return instance;
    }
    
    @Override
    protected MongoCollection<Document> setCollection() {
        return goombotioDb.getCollection(COLLECTION_NAME_KEY);
    }
    
    public void addQuote(String text, long creatorId, boolean approved) {
        long quoteId = countDocuments() + 1;
        Date created = new Date();
        Document document = new Document(ID_KEY, quoteId)
                .append(TEXT_KEY, text)
                .append(CREATOR_ID_KEY, creatorId)
                .append(CREATED_KEY, created)
                .append(APPROVED_KEY, approved);
        insertOne(document);
    }
    
    public QuoteItem getQuote(long index) {
        Document document = findFirstEquals(ID_KEY, index);
        String text = document.getString(TEXT_KEY);
        Long creatorId = document.getLong(CREATOR_ID_KEY);
        Date created = document.getDate(CREATED_KEY);
        boolean isApproved = document.getBoolean(APPROVED_KEY);
        
        return new QuoteItem(index, text, creatorId, created, isApproved);
    }
}
