package Util;

import Functions.StreamInfo;
import Util.Database.CommandDb;
import Util.Database.Entries.CommandItem;
import com.gikk.twirk.types.twitchMessage.TwitchMessage;
import com.gikk.twirk.types.users.TwitchUser;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.IOException;
import java.util.Objects;
import java.util.regex.Pattern;

public class CommandParser {
    private static final String ERROR = "ERROR";
    private static final String ERROR_URL = "-bad url-";
    private static final String ERROR_URL_RETRIEVAL = "unable to retrieve result from url";
    private static final String ERROR_BAD_ARG_NUM_FORMAT = "-bad argument number \"%s\"-";
    
    private static final String TYPE_ARG = "arg";
    private static final String TYPE_CHANNEL = "channel";
    private static final String TYPE_COUNT = "count";
    private static final String TYPE_EVAL = "eval";
    private static final String TYPE_QUERY = "query";
    private static final String TYPE_TOUSER = "touser";
    private static final String TYPE_UPTIME = "uptime";
    private static final String TYPE_URL_FETCH = "urlfetch";
    private static final String TYPE_USER_ID = "userid";
    
    private static final ScriptEngine engine = new ScriptEngineManager().getEngineByName("JavaScript");
    private static final OkHttpClient client = new OkHttpClient();
    
    private final StreamInfo streamInfo;
    private final CommandDb commandDb;
    
    public CommandParser(StreamInfo streamInfo) {
        this.streamInfo = streamInfo;
        this.commandDb = CommandDb.getInstance();
    }
    
    public String parse(CommandItem commandItem, TwitchUser user, TwitchMessage twitchMessage) {
        String output = commandItem.getMessage();
        String expression = getNextExpression(output);
        while (!expression.isEmpty()) {
            String replacement = evaluateExpression(expression, commandItem, user, twitchMessage);
            output = output.replaceFirst(Pattern.quote("$(" + expression + ")"), replacement);
            expression = getNextExpression(output);
        }
        return output;
    }
    
    private String evaluateExpression(String expression, CommandItem commandItem,
                                      TwitchUser user, TwitchMessage twitchMessage) {
        String[] split = expression.split(" ", 2);
        String type = split[0];
        String[] arguments = twitchMessage.getContent().split(" ");
        String content = "";
        if (split.length > 1) {
            content = split[1];
        }
        switch (type) {
            case TYPE_ARG:
                int arg;
                try {
                    arg = Integer.parseInt(content) + 1;
                } catch (NumberFormatException e) {
                    return String.format(ERROR_BAD_ARG_NUM_FORMAT, content);
                }
        
                if (arg < 0) {
                    return String.format(ERROR_BAD_ARG_NUM_FORMAT, content);
                }
                else  if (arguments.length > arg) {
                    return arguments[arg];
                }
                else {
                    return "";
                }
            case TYPE_CHANNEL:
                return streamInfo.getChannelName();
            case TYPE_COUNT:
                commandDb.incrementCount(commandItem.getId());
                return Integer.toString(commandItem.getCount() + 1);
            case TYPE_EVAL:
                return evalJavaScript(content);
            case TYPE_QUERY:
                if (arguments.length > 1) {
                    return twitchMessage.getContent().split(" ", 2)[1];
                }
                else {
                    return "";
                }
            case TYPE_TOUSER:
                if (arguments.length > 1) {
                    return arguments[1];
                }
                else {
                    return user.getUserName();
                }
            case TYPE_UPTIME:
                return streamInfo.getUptime();
            case TYPE_URL_FETCH:
                return submitRequest(content);
            case TYPE_USER_ID:
                return Long.toString(user.getUserID());
            default:
                return ERROR;
        }
    }
    
    private static String getNextExpression(String input) {
        try {
            for (int i = 0; i < input.length(); i++) {
                if (input.charAt(i) == '$' && input.charAt(i + 1) == '(') {
                    i += 2;
                    int start = i;
                    int depth = 1;
                    while (depth != 0) {
                        if (input.charAt(i) == '$' && input.charAt(i + 1) == '(') {
                            start = i + 2;
                            i++;
                            depth = 1;
                        }
                        else if (input.charAt(i) == '(') {
                            depth += 1;
                        }
                        else if (input.charAt(i) == ')') {
                            depth -= 1;
                        }
                        i++;
                    }
                    i -= 1;
                    return input.substring(start, i);
                }
            }
        }
        catch (IndexOutOfBoundsException e) {
            //do nothing, reached the end
        }
        return "";
    }
    
    private static String evalJavaScript(String js) {
        try {
            return engine.eval(js).toString();
        } catch (ScriptException e) {
            return ERROR;
        }
    }
    
    private static String submitRequest(String url) {
        Request request;
        try {
            request = new Request.Builder()
                    .url(url)
                    .build();
        } catch (IllegalArgumentException e) {
            return ERROR_URL;
        }
        try (Response response = client.newCall(request).execute()) {
            return Objects.requireNonNull(response.body()).string();
        } catch (IOException e) {
            return ERROR_URL_RETRIEVAL;
        }
    }
}
