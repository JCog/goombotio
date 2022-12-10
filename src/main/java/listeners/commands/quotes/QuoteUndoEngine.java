package listeners.commands.quotes;

import database.entries.QuoteItem;
import database.misc.QuoteDb;
import util.TwitchApi;

import java.util.Deque;
import java.util.LinkedList;

import static listeners.commands.quotes.QuoteUndoEngine.Action.*;

public class QuoteUndoEngine {
    private static final int STACK_MAX_SIZE = 10;

    private final Deque<QuoteUndoItem> actionUndoStack = new LinkedList<>();
    private final Deque<QuoteUndoItem> actionRedoStack = new LinkedList<>();
    private final TwitchApi twitchApi;
    private final QuoteDb quoteDb;

    public enum Action {
        ADD,
        EDIT,
        DELETE
    }

    public QuoteUndoEngine(TwitchApi twitchApi, QuoteDb quoteDb) {
        this.twitchApi = twitchApi;
        this.quoteDb = quoteDb;
    }

    public void storeUndoAction(Action action, QuoteItem quoteItem) {
        addUndoItem(action, quoteItem);
        clearRedoStack();
    }

    public void undo() {
        if (actionUndoStack.isEmpty()) {
            twitchApi.channelMessage("No quote actions to undo");
        } else {
            QuoteUndoItem quoteUndoItem = actionUndoStack.pop();
            QuoteItem quote = quoteUndoItem.getQuoteItem();
            switch (quoteUndoItem.getAction()) {
                case ADD: {
                    QuoteItem quoteToRedo = quoteDb.deleteQuote(quote.getIndex());
                    addRedoItem(ADD, quoteToRedo);
                    twitchApi.channelMessage(String.format("Quote #%d deleted", quote.getIndex()));
                    break;
                }
                case EDIT: {
                    QuoteItem quoteToRedo = quoteDb.editQuote(quote);
                    addRedoItem(EDIT, quoteToRedo);
                    twitchApi.channelMessage(String.format("Reverted edit to quote #%d", quote.getIndex()));
                    break;
                }
                case DELETE: {
                    quoteDb.reAddDeletedQuote(quote);
                    addRedoItem(quoteUndoItem.getAction(), quoteUndoItem.getQuoteItem());
                    twitchApi.channelMessage(String.format("Re-added quote #%d", quote.getIndex()));
                    break;
                }
            }
        }
    }

    public void redo() {
        if (actionRedoStack.isEmpty()) {
            twitchApi.channelMessage("No quote actions to redo");
        } else {
            QuoteUndoItem quoteRedoItem = actionRedoStack.pop();
            QuoteItem quoteToRedo = quoteRedoItem.getQuoteItem();
            switch (quoteRedoItem.getAction()) {
                case ADD: {
                    QuoteItem quoteToUndo = quoteDb.addQuote(quoteToRedo.getText(),
                                                             quoteToRedo.getCreatorId(),
                                                             quoteToRedo.getCreated(),
                                                             quoteToRedo.isApproved());
                    addUndoItem(ADD, quoteToUndo);
                    twitchApi.channelMessage(String.format("Re-added quote #%d", quoteToRedo.getIndex()));
                    break;
                }
                case EDIT: {
                    QuoteItem quoteToUndo = quoteDb.editQuote(quoteToRedo);
                    addUndoItem(EDIT, quoteToUndo);
                    twitchApi.channelMessage(String.format("Redid edit to quote #%d", quoteToRedo.getIndex()));
                    break;
                }
                case DELETE: {
                    QuoteItem quoteToUndo = quoteDb.deleteQuote(quoteToRedo.getIndex());
                    addUndoItem(DELETE, quoteToUndo);
                    twitchApi.channelMessage(String.format("Deleted quote #%d", quoteToRedo.getIndex()));
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
