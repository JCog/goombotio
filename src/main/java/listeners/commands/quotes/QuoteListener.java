package listeners.commands.quotes;

import com.github.twitch4j.chat.events.channel.ChannelMessageEvent;
import database.misc.QuoteDb;
import listeners.commands.CommandBase;
import util.CommonUtils;
import util.TwitchApi;
import util.TwitchUserLevel.USER_LEVEL;

import java.util.List;
import java.util.Random;

import static database.misc.QuoteDb.QuoteItem;
import static listeners.commands.quotes.QuoteUndoEngine.Action.*;

public class QuoteListener extends CommandBase {
    private static final CommandType COMMAND_TYPE = CommandType.PREFIX_COMMAND;
    private static final USER_LEVEL MIN_USER_LEVEL = USER_LEVEL.DEFAULT;
    private static final int COOLDOWN = 5;
    private static final CooldownType COOLDOWN_TYPE = CooldownType.COMBINED;
    private static final String PATTERN_QUOTE = "!quote";
    private static final String PATTERN_ADD_QUOTE = "!addquote";
    private static final String PATTERN_DELETE_QUOTE = "!delquote";
    private static final String PATTERN_EDIT_QUOTE = "!editquote";
    private static final String PATTERN_LATEST_QUOTE = "!latestquote";
    private static final String PATTERN_UNDO_QUOTE = "!undoquote";
    private static final String PATTERN_REDO_QUOTE = "!redoquote";
    
    private static final String ERROR_MISSING_ARGUMENTS = "Missing argument(s)";
    private static final String ERROR_NO_MATCHING_QUOTES = "No matching quotes";
    private static final String ERROR_BAD_INDEX_FORMAT = "Unable to parse quote \"%s\"";
    private static final String ERROR_NOT_LIVE = "VIPs can only add quotes while the stream is live";

    private final TwitchApi twitchApi;
    private final QuoteDb quoteDb;
    private final Random random;
    private final QuoteUndoEngine quoteUndoEngine;

    public QuoteListener(CommonUtils commonUtils) {
        super(
                COMMAND_TYPE,
                MIN_USER_LEVEL,
                COOLDOWN,
                COOLDOWN_TYPE,
                PATTERN_QUOTE,
                PATTERN_ADD_QUOTE,
                PATTERN_DELETE_QUOTE,
                PATTERN_EDIT_QUOTE,
                PATTERN_LATEST_QUOTE,
                PATTERN_UNDO_QUOTE,
                PATTERN_REDO_QUOTE
        );
        twitchApi = commonUtils.getTwitchApi();
        quoteDb = commonUtils.getDbManager().getQuoteDb();
        random = new Random();
        quoteUndoEngine = new QuoteUndoEngine(twitchApi, quoteDb);
    }

    @Override
    protected void performCommand(String command, USER_LEVEL userLevel, ChannelMessageEvent messageEvent) {
        String[] messageSplit = messageEvent.getMessage().trim().split(" ", 2);
        String content = "";
        if (messageSplit.length > 1) {
            content = messageSplit[1];
        }
        switch (command) {
            case PATTERN_QUOTE: {
                QuoteItem quote;
                if (content.isEmpty()) {
                    do {
                        long index = random.nextInt(quoteDb.getQuoteCount());
                        quote = quoteDb.getQuote(index);
                    } while (quote == null || !quote.isApproved());
                    
                } else {
                    long index;
                    try {
                        index = Long.parseLong(content);
                        quote = quoteDb.getQuote(index);
                    } catch (NumberFormatException e) {
                        List<QuoteItem> quotes = quoteDb.searchApprovedQuotes(content);
                        if (quotes.isEmpty()) {
                            twitchApi.channelMessage(ERROR_NO_MATCHING_QUOTES);
                            break;
                        }
                        int randInt = random.nextInt(quotes.size());
                        quote = quotes.get(randInt);
                    }
                }
                if (quote == null || !quote.isApproved()) {
                    twitchApi.channelMessage(ERROR_NO_MATCHING_QUOTES);
                } else {
                    twitchApi.channelMessage(quote.toString());
                }
                break;
            }
            case PATTERN_ADD_QUOTE: {
                if (userLevel.value >= USER_LEVEL.VIP.value) {
                    //only allow VIPs to add quotes if the stream is live
                    if (
                            userLevel.value == USER_LEVEL.VIP.value &&
                            twitchApi.getStreamByUsername(twitchApi.getStreamerUser().getLogin()) == null
                    ) {
                        twitchApi.channelMessage(ERROR_NOT_LIVE);
                        break;
                    }
                    if (content.isEmpty()) {
                        twitchApi.channelMessage(ERROR_MISSING_ARGUMENTS);
                        break;
                    }
                    QuoteItem quoteItem = quoteDb.addQuote(
                            content,
                            Long.parseLong(messageEvent.getUser().getId()),
                            true
                    );
                    quoteUndoEngine.storeUndoAction(ADD, quoteItem);
                    twitchApi.channelMessage(String.format("Successfully added quote #%d", quoteItem.getIndex()));
                }
                break;
            }
            case PATTERN_DELETE_QUOTE: {
                if (userLevel.value >= USER_LEVEL.MOD.value) {
                    if (content.isEmpty()) {
                        twitchApi.channelMessage(ERROR_MISSING_ARGUMENTS);
                        break;
                    }
                    long delIndex;
                    try {
                        delIndex = Long.parseLong(content);
                    } catch (NumberFormatException e) {
                        twitchApi.channelMessage(getBadIndexError(content));
                        break;
                    }
                    QuoteItem quote = quoteDb.deleteQuote(delIndex);
                    quoteUndoEngine.storeUndoAction(DELETE, quote);
                    twitchApi.channelMessage(String.format("Successfully deleted quote #%d", delIndex));
                }
                break;
            }
            case PATTERN_EDIT_QUOTE: {
                if (userLevel.value >= USER_LEVEL.MOD.value) {
                    String[] editSplit = content.split(" ", 2);
                    if (editSplit.length != 2) {
                        twitchApi.channelMessage(ERROR_MISSING_ARGUMENTS);
                        break;
                    }
                    long editIndex;
                    try {
                        editIndex = Long.parseLong(editSplit[0]);
                    } catch (NumberFormatException e) {
                        twitchApi.channelMessage(getBadIndexError(editSplit[0]));
                        break;
                    }
                    QuoteItem quote = quoteDb.editQuote(
                            editIndex,
                            editSplit[1],
                            Long.parseLong(messageEvent.getUser().getId()),
                            true
                    );
                    quoteUndoEngine.storeUndoAction(EDIT, quote);
                    twitchApi.channelMessage(String.format("Successfully edited quote #%d", editIndex));
                }
                break;
            }
            case PATTERN_LATEST_QUOTE: {
                QuoteItem quote = quoteDb.getQuote(quoteDb.getQuoteCount());
                String output = "";
                if (quote != null) {
                    output = quote.toString();
                }
                twitchApi.channelMessage(output);
                break;
            }
            case PATTERN_UNDO_QUOTE: {
                if (userLevel.value >= USER_LEVEL.MOD.value) {
                    quoteUndoEngine.undo();
                }
                break;
            }
            case PATTERN_REDO_QUOTE: {
                if (userLevel.value >= USER_LEVEL.MOD.value) {
                    quoteUndoEngine.redo();
                }
                break;
            }
        }
    }

    private String getBadIndexError(String index) {
        return String.format(ERROR_BAD_INDEX_FORMAT, index);
    }
}
