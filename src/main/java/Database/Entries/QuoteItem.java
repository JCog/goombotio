package Database.Entries;

import java.text.SimpleDateFormat;
import java.util.Date;

public class QuoteItem {
    private static final String DATE_FORMAT = "MMMM dd, yyyy";
    
    private long index;
    private String text;
    private Long creatorId;
    private Date created;
    private boolean approved;
    
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
        }
        else {
            return String.format("%d. %s, %s", index, text, getDateString());
        }
    }
    
    private String getDateString() {
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
        return sdf.format(created);
    }
}
