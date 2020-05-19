package Database.Misc;

import Database.CollectionBase;
import Database.Entries.QuoteItem;
import com.mongodb.client.MongoCollection;
import org.bson.Document;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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
    
    public String addQuote(String text, long creatorId, boolean approved) {
        long quoteId = countDocuments() + 1;
        Date created = new Date();
        Document document = new Document(ID_KEY, quoteId)
                .append(TEXT_KEY, text)
                .append(CREATOR_ID_KEY, creatorId)
                .append(CREATED_KEY, created)
                .append(APPROVED_KEY, approved);
        insertOne(document);
        return String.format("Successfully added quote %d", quoteId);
    }
    
    //be careful with this in case of intersecting IDs
    private void addQuote(QuoteItem quote) {;
        Document document = new Document(ID_KEY, quote.getIndex())
                .append(TEXT_KEY, quote.getText())
                .append(CREATOR_ID_KEY, quote.getCreatorId())
                .append(CREATED_KEY, quote.getCreated())
                .append(APPROVED_KEY, quote.isApproved());
        insertOne(document);
    }
    
    public QuoteItem getQuote(long index) {
        return convertQuoteDocument(findFirstEquals(ID_KEY, index));
    }
    
    public String deleteQuote(long index) {
        if (findFirstEquals(ID_KEY, index) != null) {
            deleteOne(index);
        }
        else {
            return getDoesNotExistString(index);
        }
        
        ArrayList<QuoteItem> replacements = new ArrayList<>();
        for (Document document : findAll()) {
            long documentIndex = document.getLong(ID_KEY);
            if (documentIndex > index) {
                document.replace(ID_KEY, documentIndex - 1);
                replacements.add(convertQuoteDocument(document));
                deleteOne(documentIndex);
            }
        }
        
        for (QuoteItem quote : replacements) {
            addQuote(quote);
        }
        return String.format("Successfully deleted quote %d", index);
    }
    
    public String editQuote(long index, String text, long userId) {
        if (findFirstEquals(ID_KEY, index) == null) {
            return getDoesNotExistString(index);
        }
        
        updateOne(index, new Document(TEXT_KEY, text));
        updateOne(index, new Document(CREATOR_ID_KEY, userId));
        updateOne(index, new Document(CREATED_KEY, new Date()));
        return String.format("Successfully edited quote %d", index);
    }
    
    public int getQuoteCount() {
        return (int) countDocuments();
    }
    
    public List<QuoteItem> searchQuotes(String query) {
        ArrayList<QuoteItem> output = new ArrayList<>();
        for (Document quote : findAll()) {
            if (quote.getString(TEXT_KEY).toLowerCase().contains(query.toLowerCase())) {
                output.add(convertQuoteDocument(quote));
            }
        }
        return output;
    }
    
    private String getDoesNotExistString(long index) {
        return String.format("ERROR: quote %d does not exist", index);
    }
    
    private QuoteItem convertQuoteDocument(Document quoteDoc) {
        if (quoteDoc == null) {
            return null;
        }
        long index = quoteDoc.getLong(ID_KEY);
        String text = quoteDoc.getString(TEXT_KEY);
        Long creatorId = quoteDoc.getLong(CREATOR_ID_KEY);
        Date created = quoteDoc.getDate(CREATED_KEY);
        boolean isApproved = quoteDoc.getBoolean(APPROVED_KEY);
        return new QuoteItem(index, text, creatorId, created, isApproved);
    }
}
