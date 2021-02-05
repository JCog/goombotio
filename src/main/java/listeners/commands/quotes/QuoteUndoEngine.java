package listeners.commands.quotes;

import com.jcog.utils.database.entries.QuoteItem;
import com.jcog.utils.database.misc.QuoteDb;
import util.TwirkInterface;

import java.util.Stack;

import static listeners.commands.quotes.QuoteUndoEngine.Action.*;

public class QuoteUndoEngine {
    private final Stack<QuoteUndoItem> actionUndoStack = new Stack<>();
    private final Stack<QuoteUndoItem> actionRedoStack = new Stack<>();
    private final TwirkInterface twirk;
    private final QuoteDb quoteDb;

    public enum Action {
        ADD,
        EDIT,
        DELETE
    }

    public QuoteUndoEngine(TwirkInterface twirk, QuoteDb quoteDb) {
        this.twirk = twirk;
        this.quoteDb = quoteDb;
    }

    public void storeUndoAction(Action action, QuoteItem quoteItem) {
        actionUndoStack.push(new QuoteUndoItem(action, quoteItem));
        clearRedoStack();
    }

    public void undo() {
        if (actionUndoStack.empty()) {
            twirk.channelMessage("No quote actions to undo");
        }
        else {
            QuoteUndoItem quoteUndoItem = actionUndoStack.pop();
            QuoteItem quote = quoteUndoItem.getQuoteItem();
            switch (quoteUndoItem.getAction()) {
                case ADD: {
                    QuoteItem quoteToRedo = quoteDb.deleteQuote(quote.getIndex());
                    actionRedoStack.push(new QuoteUndoItem(ADD, quoteToRedo));
                    twirk.channelMessage(String.format("Quote #%d deleted", quote.getIndex()));
                    break;
                }
                case EDIT: {
                    QuoteItem quoteToRedo = quoteDb.editQuote(quote);
                    actionRedoStack.push(new QuoteUndoItem(EDIT, quoteToRedo));
                    twirk.channelMessage(String.format("Reverted edit to quote #%d", quote.getIndex()));
                    break;
                }
                case DELETE: {
                    quoteDb.reAddDeletedQuote(quote);
                    actionRedoStack.push(quoteUndoItem);
                    twirk.channelMessage(String.format("Readded quote #%d", quote.getIndex()));
                    break;
                }
            }
        }
    }

    public void redo() {
        if (actionRedoStack.empty()) {
            twirk.channelMessage("No quote actions to redo");
        }
        else {
            QuoteUndoItem quoteRedoItem = actionRedoStack.pop();
            QuoteItem quoteToRedo = quoteRedoItem.getQuoteItem();
            switch (quoteRedoItem.getAction()) {
                case ADD: {
                    QuoteItem quoteToUndo = quoteDb.addQuote(quoteToRedo.getText(),
                                                             quoteToRedo.getCreatorId(),
                                                             quoteToRedo.getCreated(),
                                                             quoteToRedo.isApproved());
                    actionUndoStack.push(new QuoteUndoItem(ADD, quoteToUndo));
                    twirk.channelMessage(String.format("Re-added quote #%d", quoteToRedo.getIndex()));
                    break;
                }
                case EDIT: {
                    QuoteItem quoteToUndo = quoteDb.editQuote(quoteToRedo);
                    actionUndoStack.push(new QuoteUndoItem(EDIT, quoteToUndo));
                    twirk.channelMessage(String.format("Redid edit to quote #%d", quoteToRedo.getIndex()));
                    break;
                }
                case DELETE: {
                    QuoteItem quoteToUndo = quoteDb.deleteQuote(quoteToRedo.getIndex());
                    actionUndoStack.push(new QuoteUndoItem(DELETE, quoteToUndo));
                    twirk.channelMessage(String.format("Deleted quote #%d", quoteToRedo.getIndex()));
                    break;
                }
            }
        }
    }

    private void clearRedoStack() {
        actionRedoStack.clear();
    }
}
