package Util;

import Functions.StreamInfo;
import Util.Database.CommandDb;
import Util.Database.Entries.CommandItem;
import com.gikk.twirk.types.twitchMessage.TwitchMessage;
import com.gikk.twirk.types.users.TwitchUser;
import com.github.twitch4j.TwitchClient;
import com.github.twitch4j.helix.domain.FollowList;
import com.github.twitch4j.helix.domain.User;
import com.github.twitch4j.helix.domain.UserList;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.IOException;
import java.time.LocalDate;
import java.time.Period;
import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
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
    private static final String TYPE_FOLLOW_AGE = "followage";
    private static final String TYPE_QUERY = "query";
    private static final String TYPE_TOUSER = "touser";
    private static final String TYPE_UPTIME = "uptime";
    private static final String TYPE_URL_FETCH = "urlfetch";
    private static final String TYPE_USER_ID = "userid";
    
    private static final ScriptEngine engine = new ScriptEngineManager().getEngineByName("JavaScript");
    private static final OkHttpClient client = new OkHttpClient();
    
    private final TwitchClient twitchClient;
    private final StreamInfo streamInfo;
    private final CommandDb commandDb;
    private final String authToken;
    
    public CommandParser(String authToken, TwitchClient twitchClient, StreamInfo streamInfo) {
        this.authToken = authToken;
        this.twitchClient = twitchClient;
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
            case TYPE_FOLLOW_AGE:
                if (content.split(" ").length == 1) {
                    return getFollowAgeString(content);
                }
                else {
                    return ERROR;
                }
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
                long uptimeSeconds = streamInfo.getUptime();
                if (uptimeSeconds == -1) {
                    return "stream is not live";
                }
                else {
                    return getTimeString(uptimeSeconds);
                }
            case TYPE_URL_FETCH:
                return submitRequest(content);
            case TYPE_USER_ID:
                return Long.toString(user.getUserID());
            default:
                return ERROR;
        }
    }
    
    private String getFollowAgeString(String userName) {
        UserList userList = twitchClient.getHelix().getUsers(authToken, null, Collections.singletonList(userName)).execute();
        if (userList.getUsers().size() == 0) {
            return String.format("Unknown user \"%s\"", userName);
        }
        User user = userList.getUsers().get(0);
        FollowList followList = twitchClient.getHelix().getFollowers(
                authToken,
                user.getId(),
                streamInfo.getChannelId(),
                null,
                1
        ).execute();
        
        if(followList.getFollows().size() > 0) {
            LocalDate followDate = followList.getFollows().get(0).getFollowedAt().toLocalDate();
            LocalDate today = LocalDate.now();
            Period period = Period.between(followDate, today);
            int years = period.getYears();
            int months = period.getMonths();
            int days = period.getDays();
            if (years == 0 && months == 0 && days == 0) {
                return String.format("%s followed %s today", user.getDisplayName(), streamInfo.getChannelDisplayName());
            }
            StringBuilder timeString = new StringBuilder();
            timeString.append(years > 0 ? String.format("%d year%s, ", years, years > 1 ? "s" : "") : "");
            timeString.append(months > 0 ? String.format("%d month%s, ", months, months > 1 ? "s" : "") : "");
            timeString.append(days > 0 ? String.format("%d day%s", days, days > 1 ? "s" : "") : "");
            if (timeString.charAt(timeString.length() - 1) == ' ') {
                timeString.deleteCharAt(timeString.length() - 1);
                timeString.deleteCharAt(timeString.length() - 1);
            }
            return String.format(
                    "%s has been following %s for %s",
                    user.getDisplayName(),
                    streamInfo.getChannelDisplayName(),
                    timeString.toString()
            );
        }
        else {
            return String.format("%s is not following %s", user.getDisplayName(), streamInfo.getChannelDisplayName());
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
    
    private static String getTimeString(long seconds) {
        long hours = TimeUnit.SECONDS.toHours(seconds);
        seconds -= TimeUnit.HOURS.toSeconds(hours);
        long minutes = TimeUnit.SECONDS.toMinutes(seconds);
        seconds -= TimeUnit.MINUTES.toSeconds(minutes);
        if (hours > 0) {
            return String.format("%d hours, %d minutes, %d seconds", hours, minutes, seconds);
        }
        else if (minutes > 0){
            return String.format("%d minutes, %d seconds", minutes, seconds);
        }
        else {
            return String.format("%d seconds", seconds);
        }
    }
}
