package Listeners.Commands;

import Util.Database.Entries.QuoteItem;
import Util.Database.QuoteDb;
import Util.TwirkInterface;
import com.gikk.twirk.enums.USER_TYPE;
import com.gikk.twirk.types.twitchMessage.TwitchMessage;
import com.gikk.twirk.types.users.TwitchUser;

import java.util.List;
import java.util.Random;

public class QuoteListener extends CommandBase {
    
    private static final String PATTERN_QUOTE = "!quote";
    private static final String PATTERN_ADD_QUOTE = "!addquote";
    private static final String PATTERN_DELETE_QUOTE = "!delquote";
    private static final String PATTERN_EDIT_QUOTE = "!editquote";
    private static final String ERROR_MISSING_ARGUMENTS = "Missing argument(s)";
    private static final String ERROR_NO_MATCHING_QUOTES = "No matching quotes";
    private static final String ERROR_BAD_INDEX_FORMAT = "Unable to parse quote \"%s\"";
    
    private final TwirkInterface twirk;
    private final QuoteDb quoteDb;
    private final Random random;
    
    public QuoteListener(TwirkInterface twirk) {
        super(CommandType.PREFIX_COMMAND);
        this.twirk = twirk;
        quoteDb = QuoteDb.getInstance();
        random = new Random();
    }
    
    @Override
    protected String getCommandWords() {
        return String.join("|",
                PATTERN_QUOTE,
                PATTERN_ADD_QUOTE,
                PATTERN_DELETE_QUOTE,
                PATTERN_EDIT_QUOTE
        );
    }
    
    @Override
    protected USER_TYPE getMinUserPrivilege() {
        return USER_TYPE.DEFAULT;
    }
    
    @Override
    protected int getCooldownLength() {
        return 5 * 1000;
    }
    
    @Override
    protected void performCommand(String command, TwitchUser sender, TwitchMessage message) {
        String[] messageSplit = message.getContent().trim().split(" ", 2);
        String content = "";
        if (messageSplit.length > 1) {
            content = messageSplit[1];
        }
        switch (command) {
            case PATTERN_QUOTE:
                QuoteItem quote;
                if (content.isEmpty()) {
                    long index = random.nextInt(quoteDb.getQuoteCount());
                    quote = quoteDb.getQuote(index);
                }
                else {
                    long index;
                    try {
                        index = Long.parseLong(content);
                        quote = quoteDb.getQuote(index);
                    }
                    catch (NumberFormatException e) {
                        List<QuoteItem> quotes = quoteDb.searchQuotes(content);
                        if (quotes.isEmpty()) {
                            twirk.channelMessage(ERROR_NO_MATCHING_QUOTES);
                            break;
                        }
                        int randInt = random.nextInt(quotes.size());
                        quote = quotes.get(randInt);
                    }
                }
                if (quote == null) {
                    twirk.channelMessage(ERROR_NO_MATCHING_QUOTES);
                }
                else {
                    twirk.channelMessage(quote.toString());
                }
                break;
            //TODO: once Twirk supports checking for VIP status, allow VIPs to add unapproved quotes
            case PATTERN_ADD_QUOTE:
                if (sender.getUserType().value >= USER_TYPE.MOD.value) {
                    if (content.isEmpty()) {
                        twirk.channelMessage(ERROR_MISSING_ARGUMENTS);
                        break;
                    }
                    twirk.channelMessage(quoteDb.addQuote(content, sender.getUserID(), true));
                }
                break;
            case PATTERN_DELETE_QUOTE:
                if (sender.getUserType().value >= USER_TYPE.MOD.value) {
                    if (content.isEmpty()) {
                        twirk.channelMessage(ERROR_MISSING_ARGUMENTS);
                        break;
                    }
                    long delIndex;
                    try {
                        delIndex = Long.parseLong(content);
                    }
                    catch (NumberFormatException e) {
                        twirk.channelMessage(getBadIndexError(content));
                        break;
                    }
                    twirk.channelMessage(quoteDb.deleteQuote(delIndex));
                }
                break;
            case PATTERN_EDIT_QUOTE:
                if (sender.getUserType().value >= USER_TYPE.MOD.value) {
                    String[] editSplit = content.split(" ", 2);
                    if (editSplit.length != 2) {
                        twirk.channelMessage(ERROR_MISSING_ARGUMENTS);
                        break;
                    }
                    long editIndex;
                    try {
                        editIndex = Long.parseLong(editSplit[0]);
                    }
                    catch (NumberFormatException e) {
                        twirk.channelMessage(getBadIndexError(editSplit[0]));
                        break;
                    }
                    twirk.channelMessage(quoteDb.editQuote(editIndex, editSplit[1], sender.getUserID()));
                }
        }
    }
    
    private String getBadIndexError(String index) {
        return String.format(ERROR_BAD_INDEX_FORMAT, index);
    }
}
