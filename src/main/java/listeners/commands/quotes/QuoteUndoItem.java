package listeners.commands.quotes;

import com.jcog.utils.database.entries.QuoteItem;

public class QuoteUndoItem {
    private final QuoteUndoEngine.Action action;
    private final QuoteItem quoteItem;

    public QuoteUndoItem(QuoteUndoEngine.Action action, QuoteItem quoteItem) {
        this.action = action;
        this.quoteItem = quoteItem;
    }

    public QuoteUndoEngine.Action getAction() {
        return action;
    }

    public QuoteItem getQuoteItem() {
        return quoteItem;
    }
}
