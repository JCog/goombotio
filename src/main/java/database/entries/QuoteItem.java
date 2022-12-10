package database.entries;

import java.text.SimpleDateFormat;
import java.util.Date;

public class QuoteItem {
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
