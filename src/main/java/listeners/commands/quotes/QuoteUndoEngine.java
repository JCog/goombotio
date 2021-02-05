package listeners.commands.quotes;

import com.jcog.utils.database.entries.QuoteItem;
import com.jcog.utils.database.misc.QuoteDb;
import util.TwirkInterface;

import java.util.Deque;
import java.util.LinkedList;

import static listeners.commands.quotes.QuoteUndoEngine.Action.*;

public class QuoteUndoEngine {
    private static final int STACK_MAX_SIZE = 10;

    private final Deque<QuoteUndoItem> actionUndoStack = new LinkedList<>();
    private final Deque<QuoteUndoItem> actionRedoStack = new LinkedList<>();
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
        addUndoItem(action, quoteItem);
        clearRedoStack();
    }

    public void undo() {
        if (actionUndoStack.isEmpty()) {
            twirk.channelMessage("No quote actions to undo");
        }
        else {
            QuoteUndoItem quoteUndoItem = actionUndoStack.pop();
            QuoteItem quote = quoteUndoItem.getQuoteItem();
            switch (quoteUndoItem.getAction()) {
                case ADD: {
                    QuoteItem quoteToRedo = quoteDb.deleteQuote(quote.getIndex());
                    addRedoItem(ADD, quoteToRedo);
                    twirk.channelMessage(String.format("Quote #%d deleted", quote.getIndex()));
                    break;
                }
                case EDIT: {
                    QuoteItem quoteToRedo = quoteDb.editQuote(quote);
                    addRedoItem(EDIT, quoteToRedo);
                    twirk.channelMessage(String.format("Reverted edit to quote #%d", quote.getIndex()));
                    break;
                }
                case DELETE: {
                    quoteDb.reAddDeletedQuote(quote);
                    addRedoItem(quoteUndoItem.getAction(), quoteUndoItem.getQuoteItem());
                    twirk.channelMessage(String.format("Readded quote #%d", quote.getIndex()));
                    break;
                }
            }
        }
    }

    public void redo() {
        if (actionRedoStack.isEmpty()) {
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
                    addUndoItem(ADD, quoteToUndo);
                    twirk.channelMessage(String.format("Re-added quote #%d", quoteToRedo.getIndex()));
                    break;
                }
                case EDIT: {
                    QuoteItem quoteToUndo = quoteDb.editQuote(quoteToRedo);
                    addUndoItem(EDIT, quoteToUndo);
                    twirk.channelMessage(String.format("Redid edit to quote #%d", quoteToRedo.getIndex()));
                    break;
                }
                case DELETE: {
                    QuoteItem quoteToUndo = quoteDb.deleteQuote(quoteToRedo.getIndex());
                    addUndoItem(DELETE, quoteToUndo);
                    twirk.channelMessage(String.format("Deleted quote #%d", quoteToRedo.getIndex()));
                    break;
                }
            }
        }
    }

    private void clearRedoStack() {
        actionRedoStack.clear();
    }

    private void addUndoItem(Action action, QuoteItem quoteItem) {
        actionUndoStack.push(new QuoteUndoItem(action, quoteItem));
        while (actionUndoStack.size() > STACK_MAX_SIZE) {
            actionUndoStack.removeLast();
        }
    }

    private void addRedoItem(Action action, QuoteItem quoteItem) {
        actionRedoStack.push(new QuoteUndoItem(action, quoteItem));
        while (actionRedoStack.size() > STACK_MAX_SIZE) {
            actionRedoStack.removeLast();
        }
    }
}
