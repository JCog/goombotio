package database.misc;

import database.GbCollection;
import database.GbDatabase;
import org.bson.Document;
import org.jetbrains.annotations.Nullable;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class QuoteDb extends GbCollection {
    private static final String COLLECTION_NAME = "quotes";
    
    private static final String TEXT_KEY = "text";
    private static final String CREATOR_ID_KEY = "creator_id";
    private static final String CREATED_KEY = "created";
    private static final String APPROVED_KEY = "approved";

    public QuoteDb(GbDatabase gbDatabase) {
        super(gbDatabase, COLLECTION_NAME);
    }

    public QuoteItem addQuote(String text, long creatorId, boolean approved) {
        Date creationDate = new Date();
        return addQuote(text, creatorId, creationDate, approved);
    }

    public QuoteItem addQuote(String text, long creatorId, Date creationDate, boolean approved) {
        long quoteId = countDocuments() + 1;
        Document document = new Document(ID_KEY, quoteId)
                .append(TEXT_KEY, text)
                .append(CREATOR_ID_KEY, creatorId)
                .append(CREATED_KEY, creationDate)
                .append(APPROVED_KEY, approved);
        insertOne(document);
        return convertQuoteDocument(document);
    }

    public void reAddDeletedQuote(QuoteItem quote) {
        for (Document nextQuote : sortDescending(findAll(), ID_KEY)) {
            long oldId = nextQuote.getLong(ID_KEY);
            if (oldId < quote.getIndex()) {
                break;
            }
            deleteOne(oldId);
            addQuote(new QuoteItem(oldId + 1,
                                   nextQuote.getString(TEXT_KEY),
                                   nextQuote.getLong(CREATOR_ID_KEY),
                                   nextQuote.getDate(CREATED_KEY),
                                   nextQuote.getBoolean(APPROVED_KEY)));
            updateOne(oldId, new Document(ID_KEY, oldId + 1));
        }
        addQuote(quote);
    }

    //be careful with this in case of intersecting IDs
    private void addQuote(QuoteItem quote) {
        Document document = new Document(ID_KEY, quote.getIndex())
                .append(TEXT_KEY, quote.getText())
                .append(CREATOR_ID_KEY, quote.getCreatorId())
                .append(CREATED_KEY, quote.getCreated())
                .append(APPROVED_KEY, quote.isApproved());
        insertOne(document);
    }

    @Nullable
    public QuoteItem getQuote(long index) {
        return convertQuoteDocument(findFirstEquals(ID_KEY, index));
    }

    @Nullable
    public QuoteItem deleteQuote(long index) {
        Document quoteToDelete = findFirstEquals(ID_KEY, index);
        if (quoteToDelete != null) {
            deleteOne(index);
        } else {
            return null;
        }

        List<QuoteItem> replacements = new ArrayList<>();
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
        return convertQuoteDocument(quoteToDelete);
    }

    @Nullable
    public QuoteItem editQuote(long index, String text, long userId, boolean approved) {
        Document quoteToEdit = findFirstEquals(ID_KEY, index);
        if (quoteToEdit == null) {
            return null;
        }

        updateOne(index, new Document(TEXT_KEY, text));
        updateOne(index, new Document(CREATOR_ID_KEY, userId));
        updateOne(index, new Document(APPROVED_KEY, approved));
        return convertQuoteDocument(quoteToEdit);
    }

    @Nullable
    public QuoteItem editQuote(QuoteItem quote) {
        return editQuote(quote.getIndex(), quote.getText(), quote.getCreatorId(), quote.isApproved());
    }

    public int getQuoteCount() {
        return (int) countDocuments();
    }

    public List<QuoteItem> searchApprovedQuotes(String query) {
        List<QuoteItem> output = new ArrayList<>();
        for (Document quote : findContainsSubstring(TEXT_KEY, query, false)) {
            QuoteItem quoteItem = convertQuoteDocument(quote);
            if (quoteItem!= null && quoteItem.isApproved()) {
                output.add(quoteItem);
            }
        }
        return output;
    }

    private String getDoesNotExistString(long index) {
        return String.format("ERROR: quote %d does not exist", index);
    }

    @Nullable
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
    
    public static class QuoteItem {
        private static final String DATE_FORMAT = "MMMM dd, yyyy";
        
        private final long index;
        private final String text;
        private final Long creatorId;
        private final Date created;
        private final boolean approved;
        
        public QuoteItem(long index, String text, Long creatorId, Date created, boolean approved) {
            this.index = index;
            this.text = text;
            this.creatorId = creatorId;
            this.created = created;
            this.approved = approved;
        }
        
        public long getIndex() {
            return index;
        }
        
        public String getText() {
            return text;
        }
        
        public Long getCreatorId() {
            return creatorId;
        }
        
        public Date getCreated() {
            return created;
        }
        
        public boolean isApproved() {
            return approved;
        }
        
        @Override
        public String toString() {
            if (created == null) {
                return String.format("%d. %s", index, text);
            } else {
                return String.format("%d. %s, %s", index, text, getDateString());
            }
        }
        
        private String getDateString() {
            SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
            return sdf.format(created);
        }
    }
}
