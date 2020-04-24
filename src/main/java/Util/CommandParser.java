package Util;

import Functions.StreamInfo;
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
    private static final String TYPE_EVAL = "eval";
    private static final String TYPE_TOUSER = "touser";
    private static final String TYPE_CHANNEL = "channel";
    private static final String TYPE_URL_FETCH = "urlfetch";
    private static final String TYPE_UPTIME = "uptime";
    
    private static final ScriptEngine engine = new ScriptEngineManager().getEngineByName("JavaScript");
    private static final OkHttpClient client = new OkHttpClient();
    
    private final StreamInfo streamInfo;
    
    public CommandParser(StreamInfo streamInfo) {
        this.streamInfo = streamInfo;
    }
    
    public String parse(String input, TwitchUser user) {
        String output = input;
        String expression = getNextExpression(output);
        while (!expression.isEmpty()) {
            String replacement = evaluateExpression(expression, user);
            output = output.replaceFirst(Pattern.quote("$(" + expression + ")"), replacement);
            expression = getNextExpression(output);
        }
        return output;
    }
    
    private String evaluateExpression(String expression, TwitchUser user) {
        String[] split = expression.split(" ", 2);
        String type = split[0];
        String content = "";
        if (split.length > 1) {
            content = split[1];
        }
        switch (type) {
            case TYPE_EVAL:
                return evalJavaScript(content);
            case TYPE_TOUSER:
                return user.getUserName();
            case TYPE_CHANNEL:
                return streamInfo.getChannelName();
            case TYPE_URL_FETCH:
                return submitRequest(content);
            case TYPE_UPTIME:
                return streamInfo.getUptime();
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
