package util;

import com.fathzer.soft.javaluator.DoubleEvaluator;
import com.gikk.twirk.types.twitchMessage.TwitchMessage;
import com.gikk.twirk.types.users.TwitchUser;
import com.github.twitch4j.helix.domain.Follow;
import com.github.twitch4j.helix.domain.Stream;
import com.github.twitch4j.helix.domain.User;
import com.jcog.utils.TwitchApi;
import com.jcog.utils.database.DbManager;
import com.jcog.utils.database.entries.CommandItem;
import com.jcog.utils.database.misc.CommandDb;
import com.netflix.hystrix.exception.HystrixRuntimeException;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommandParser {
    private static final int EVAL_LIMIT = 10;

    private static final String ERROR = "ERROR";
    private static final String ERROR_EVAL_LIMIT_EXCEEDED = "-parse limit exceeded, probably stuck in an endless loop-";
    private static final String ERROR_MISSING_ARGUMENT = "-missing argument-";
    private static final String ERROR_COMMAND_DNE = "-command \"%s\" does not exist-";
    private static final String ERROR_URL = "-bad url-";
    private static final String ERROR_URL_RETRIEVAL = "-unable to retrieve result from url-";
    private static final String ERROR_BAD_ARG_NUM_FORMAT = "-bad argument number \"%s\"-";
    private static final String ERROR_INVALID_USER_ARG = "-invalid user argument-";
    private static final String ERROR_INVALID_RANGE = "-invalid range\"%s\"-";
    private static final String ERROR_BAD_ENTRY = "-bad entry-";
    private static final String ERROR_INVALID_WEIGHT = "-invalid weight-";

    private static final String TYPE_ALIAS = "alias";
    private static final String TYPE_ARG = "arg";
    private static final String TYPE_CHANNEL = "channel";
    private static final String TYPE_CHOOSE = "choose";
    private static final String TYPE_COUNT = "count";
    private static final String TYPE_FOLLOW_AGE = "followage";
    private static final String TYPE_EVAL = "eval";
    private static final String TYPE_QUERY = "query";
    private static final String TYPE_RAND = "rand";
    private static final String TYPE_TOUSER = "touser";
    private static final String TYPE_UPTIME = "uptime";
    private static final String TYPE_USER = "user";
    private static final String TYPE_URL_FETCH = "urlfetch";
    private static final String TYPE_USER_ID = "userid";
    private static final String TYPE_WEIGHTED = "weighted";

    private static final OkHttpClient client = new OkHttpClient();

    private final Random random = new Random();

    private final CommandDb commandDb;
    private final TwitchApi twitchApi;
    private final User streamerUser;

    public CommandParser(DbManager dbManager, TwitchApi twitchApi, User streamerUser) {
        this.twitchApi = twitchApi;
        this.streamerUser = streamerUser;
        commandDb = dbManager.getCommandDb();
    }

    public String parse(CommandItem commandItem, TwitchUser user, TwitchMessage twitchMessage) {
        String output = commandItem.getMessage();
        String expression = getNextExpression(output);
        int evalCount = 0;
        while (!expression.isEmpty()) {
            if (evalCount == EVAL_LIMIT) {
                return ERROR_EVAL_LIMIT_EXCEEDED;
            }
            String replacement = evaluateExpression(expression, commandItem, user, twitchMessage);
            //TODO: check to see if replaceFirst causes a bug
            output = output.replaceFirst(
                    Pattern.quote("$(" + expression + ")"),
                    Matcher.quoteReplacement(replacement)
            );
            expression = getNextExpression(output);
            evalCount++;
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
            case TYPE_ALIAS: {
                if (content.isEmpty()) {
                    return ERROR_MISSING_ARGUMENT;
                }

                String commandId = content.split(" ", 2)[0];
                CommandItem aliasCommand = commandDb.getCommandItem(commandId);
                if (aliasCommand == null) {
                    return String.format(ERROR_COMMAND_DNE, commandId);
                }

                return aliasCommand.getMessage();
            }
            case TYPE_ARG: {
                int arg;
                try {
                    arg = Integer.parseInt(content) + 1;
                }
                catch (NumberFormatException e) {
                    return String.format(ERROR_BAD_ARG_NUM_FORMAT, content);
                }

                if (arg < 0) {
                    return String.format(ERROR_BAD_ARG_NUM_FORMAT, content);
                }
                else if (arguments.length > arg) {
                    if (arguments[arg].contains("(") || arguments[arg].contains(")")) {
                        return ERROR_INVALID_USER_ARG;
                    }
                    return arguments[arg];
                }
                else {
                    return "";
                }
            }
            case TYPE_CHANNEL: {
                return streamerUser.getLogin();
            }
            case TYPE_CHOOSE: {
                if (content.isEmpty()) {
                    return ERROR_MISSING_ARGUMENT;
                }
                String[] entries = content.split("\\|");
                return entries[random.nextInt(entries.length)];
            }
            case TYPE_COUNT: {
                commandDb.incrementCount(commandItem.getId());
                return Integer.toString(commandItem.getCount() + 1);
            }
            case TYPE_FOLLOW_AGE: {
                if (content.split(" ").length == 1) {
                    return getFollowAgeString(content);
                }
                else {
                    return ERROR;
                }
            }
            case TYPE_EVAL: {
                DoubleEvaluator evaluator = new DoubleEvaluator();
                try {
                    DecimalFormat df = new DecimalFormat("#.##");
                    df.setRoundingMode(RoundingMode.HALF_UP);
                    return df.format(evaluator.evaluate(content));
                }
                catch (IllegalArgumentException e) {
                    return String.format("%s: %s", ERROR, e.getMessage());
                }
            }
            case TYPE_QUERY: {
                if (arguments.length > 1) {
                    return twitchMessage.getContent().split(" ", 2)[1];
                }
                else {
                    return "";
                }
            }
            case TYPE_RAND: {
                String[] rangeSplit = content.split(",");
                if (rangeSplit.length != 2) {
                    return String.format(ERROR_INVALID_RANGE, content);
                }
                int low;
                int high;
                try {
                    low = Integer.parseInt(rangeSplit[0]);
                    high = Integer.parseInt(rangeSplit[1]);
                }
                catch (NumberFormatException e) {
                    return String.format(ERROR_INVALID_RANGE, content);
                }
                if (low >= high) {
                    return String.format(ERROR_INVALID_RANGE, content);
                }
                int randomOutput = random.nextInt(high - low + 1) + low;
                return Integer.toString(randomOutput);
            }
            case TYPE_TOUSER: {
                if (arguments.length > 1) {
                    return arguments[1];
                }
                else {
                    return user.getDisplayName();
                }
            }
            case TYPE_UPTIME: {
                Stream stream;
                try {
                    stream = twitchApi.getStream(streamerUser.getLogin());
                }
                catch (HystrixRuntimeException e) {
                    e.printStackTrace();
                    return "error retrieving stream data";
                }
                if (stream == null) {
                    return "stream is not live";
                }
                else {
                    return getTimeString(stream.getUptime().toMillis() / 1000);
                }
            }
            case TYPE_URL_FETCH: {
                return submitRequest(content);
            }
            case TYPE_USER: {
                return user.getDisplayName();
            }
            case TYPE_USER_ID: {
                return Long.toString(user.getUserID());
            }
            case TYPE_WEIGHTED: {
                String[] entries = content.split("\\|");
                ArrayList<Integer> weights = new ArrayList<>();
                ArrayList<String> messages = new ArrayList<>();
                for (String entry : entries) {
                    String[] weightMessage = entry.split("\\s", 2);
                    if (weightMessage.length != 2) {
                        return ERROR_BAD_ENTRY;
                    }

                    int weight;
                    try {
                        weight = Integer.parseInt(weightMessage[0]);
                    }
                    catch (NumberFormatException e) {
                        return ERROR_INVALID_WEIGHT;
                    }

                    weights.add(weight);
                    messages.add(weightMessage[1]);
                }

                int totalWeight = weights.stream().mapToInt(a -> a).sum();
                int selection = random.nextInt(totalWeight);
                for (int i = 0; i < weights.size(); i++) {
                    if (selection < weights.get(i)) {
                        return messages.get(i);
                    }
                    else {
                        selection -= weights.get(i);
                    }
                }
                return ERROR;
            }
            default: {
                return ERROR;
            }
        }
    }

    private String getFollowAgeString(String userName) {
        User user;
        try {
            user = twitchApi.getUserByUsername(userName);
        }
        catch (HystrixRuntimeException e) {
            e.printStackTrace();
            return String.format("Error retrieving user data for %s", userName);
        }
        if (user == null) {
            return String.format("Unknown user \"%s\"", userName);
        }
        Follow follow;
        try {
            follow = twitchApi.getFollow(user.getId(), streamerUser.getId());
        }
        catch (HystrixRuntimeException e) {
            e.printStackTrace();
            return String.format("Error retrieving follow age for %s", userName);
        }

        if (follow != null) {
            LocalDate followDate = follow.getFollowedAtInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            LocalDate today = LocalDate.now();
            Period period = Period.between(followDate, today);
            int years = period.getYears();
            int months = period.getMonths();
            int days = period.getDays();
            if (years == 0 && months == 0 && days == 0) {
                return String.format(
                        "%s followed %s today",
                        user.getDisplayName(),
                        streamerUser.getDisplayName()
                );
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
                    streamerUser.getDisplayName(),
                    timeString.toString()
            );
        }
        else {
            return String.format(
                    "%s is not following %s",
                    user.getDisplayName(),
                    streamerUser.getDisplayName()
            );
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

    private static String submitRequest(String url) {
        Request request;
        try {
            request = new Request.Builder()
                    .url(url)
                    .build();
        }
        catch (IllegalArgumentException e) {
            return ERROR_URL;
        }
        try (Response response = client.newCall(request).execute()) {
            return Objects.requireNonNull(response.body()).string();
        }
        catch (IOException e) {
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
        else if (minutes > 0) {
            return String.format("%d minutes, %d seconds", minutes, seconds);
        }
        else {
            return String.format("%d seconds", seconds);
        }
    }
}
