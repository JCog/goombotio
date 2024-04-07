package util;

import com.fathzer.soft.javaluator.DoubleEvaluator;
import com.github.twitch4j.helix.domain.InboundFollow;
import com.github.twitch4j.helix.domain.Stream;
import com.github.twitch4j.helix.domain.User;
import com.netflix.hystrix.exception.HystrixRuntimeException;
import database.misc.CommandDb;
import database.misc.SocialSchedulerDb.ScheduledMessage;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static database.misc.CommandDb.CommandItem;

public class MessageExpressionParser {
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
    private static final String ERROR_NON_COMMAND = "-this expression is only for commands-";

    private static final String TYPE_ARG = "arg";
    private static final String TYPE_CHANNEL = "channel";
    private static final String TYPE_CHOOSE = "choose";
    private static final String TYPE_COMMAND = "command";
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

    public MessageExpressionParser(CommonUtils commonUtils) {
        twitchApi = commonUtils.twitchApi();
        commandDb = commonUtils.dbManager().getCommandDb();
    }
    
    public String parseScheduledMessage(ScheduledMessage scheduledMessage) {
        return parseInternal(scheduledMessage.message(), null, null, null, null, 0);
    }
    
    public String parseCommandMessage(CommandItem commandItem, String userInput, String userId, String displayName) {
        return parseInternal(commandItem.message(), commandItem, userInput, userId, displayName, 0);
    }
    
    private String parseInternal(
            String message,
            @Nullable CommandItem commandItem,
            @Nullable String userInput,
            @Nullable String userId,
            @Nullable String displayName,
            int evalDepth
    ) {
        if (evalDepth >= EVAL_LIMIT) {
            return ERROR_EVAL_LIMIT_EXCEEDED;
        }
        Deque<Integer> expressionStarts = new ArrayDeque<>();
        record Range(int start, int end) {}
        List<Range> expressionRanges = new ArrayList<>();
        record RangeIndex(int priority, int index) {}
        PriorityQueue<RangeIndex> expressionQueue = new PriorityQueue<>((a,b) -> b.priority() - a.priority());
        String output = message;
        int depth = 0;
        int index = 0;
        for (int i = 0; i < message.length(); i++) {
            if (message.charAt(i) == '$' && i != message.length() - 1 && message.charAt(i + 1) == '(') {
                expressionStarts.push(i);
                depth++;
            } else if (message.charAt(i) == ')' && !expressionStarts.isEmpty()) {
                expressionRanges.add(new Range(expressionStarts.pop(), i + 1));
                expressionQueue.offer(new RangeIndex(depth, index++));
            }
        }
        while (!expressionQueue.isEmpty()) {
            Range range = expressionRanges.get(expressionQueue.poll().index());
            int start = range.start();
            int end = range.end();
            String expression = output.substring(start + 2, end - 1);
            
            String replacement = parseExpression(expression, commandItem, userInput, userId, displayName, evalDepth);
            output = output.substring(0, start) + replacement + output.substring(end);
            
            int difference = end - start - replacement.length();
            for (int i = 0; i < expressionRanges.size(); i++) {
                Range oldRange = expressionRanges.get(i);
                int newStart = oldRange.start() > start ? oldRange.start() - difference : oldRange.start();
                int newEnd = oldRange.end() > start ? oldRange.end() - difference : oldRange.end();
                expressionRanges.set(i, new Range(newStart, newEnd));
            }
        }
        return output;
    }
    
    private String parseExpression(
            String expression,
            @Nullable CommandItem commandItem,
            @Nullable String userInput,
            @Nullable String userId,
            @Nullable String displayName,
            int evalDepth
    ) {
        String[] userArgs = userInput == null ? new String[]{} : userInput.split("\\s");
        String[] split = expression.split("\\s", 2);
        String type = split[0];
        String content = "";
        if (split.length > 1) {
            content = split[1];
        }
        switch (type) {
            case TYPE_ARG -> {
                if (commandItem == null) {
                    return ERROR_NON_COMMAND;
                }
            
                int arg;
                try {
                    arg = Integer.parseInt(content);
                } catch (NumberFormatException e) {
                    return String.format(ERROR_BAD_ARG_NUM_FORMAT, content);
                }
            
                if (arg < 0) {
                    return String.format(ERROR_BAD_ARG_NUM_FORMAT, content);
                } else if (userArgs.length > arg) {
                    if (userArgs[arg].contains("(") || userArgs[arg].contains(")")) {
                        return ERROR_INVALID_USER_ARG;
                    }
                    return userArgs[arg];
                } else {
                    return "";
                }
            }
            case TYPE_CHANNEL -> {
                return twitchApi.getStreamerUser().getLogin();
            }
            case TYPE_CHOOSE -> {
                if (content.isEmpty()) {
                    return ERROR_MISSING_ARGUMENT;
                }
                String[] entries = content.split("\\|");
                return entries[random.nextInt(entries.length)];
            }
            case TYPE_COMMAND -> {
                if (content.isEmpty()) {
                    return ERROR_MISSING_ARGUMENT;
                }
            
                String commandId = content.split("\\s", 2)[0];
                CommandItem aliasCommand = commandDb.getCommandItem(commandId);
                if (aliasCommand == null) {
                    return String.format(ERROR_COMMAND_DNE, commandId);
                }
            
                return parseInternal(
                        aliasCommand.message(), commandItem,
                        userInput,
                        userId,
                        displayName,
                        evalDepth + 1
                );
            }
            case TYPE_COUNT -> {
                if (commandItem == null) {
                    return ERROR_NON_COMMAND;
                }
                commandDb.incrementCount(commandItem.aliases().get(0));
                return Integer.toString(commandItem.count() + 1);
            }
            case TYPE_FOLLOW_AGE -> {
                if (content.split("\\s").length == 1) {
                    return getFollowAgeString(content);
                } else {
                    return ERROR;
                }
            }
            case TYPE_EVAL -> {
                DoubleEvaluator evaluator = new DoubleEvaluator();
                try {
                    DecimalFormat df = new DecimalFormat("#.##");
                    df.setRoundingMode(RoundingMode.HALF_UP);
                    return df.format(evaluator.evaluate(content));
                } catch (IllegalArgumentException e) {
                    return String.format("%s: %s", ERROR, e.getMessage());
                }
            }
            case TYPE_QUERY -> {
                if (commandItem == null) {
                    return ERROR_NON_COMMAND;
                }
                return userInput;
            }
            case TYPE_RAND -> {
                String[] rangeSplit = content.split(",");
                if (rangeSplit.length != 2) {
                    return String.format(ERROR_INVALID_RANGE, content);
                }
                int low;
                int high;
                try {
                    low = Integer.parseInt(rangeSplit[0]);
                    high = Integer.parseInt(rangeSplit[1]);
                } catch (NumberFormatException e) {
                    return String.format(ERROR_INVALID_RANGE, content);
                }
                if (low >= high) {
                    return String.format(ERROR_INVALID_RANGE, content);
                }
                int randomOutput = random.nextInt(high - low + 1) + low;
                return Integer.toString(randomOutput);
            }
            case TYPE_TOUSER -> {
                if (commandItem == null) {
                    return ERROR_NON_COMMAND;
                }
                if (userArgs.length != 0) {
                    String username = userArgs[0].startsWith("@") ? userArgs[0].substring(1) : userArgs[0];
                    User user;
                    try {
                        user = twitchApi.getUserByUsername(username);
                    } catch (HystrixRuntimeException e) {
                        user = null;
                    }
                    return user == null ? username : user.getDisplayName();
                } else {
                    return displayName;
                }
            }
            case TYPE_UPTIME -> {
                Stream stream;
                try {
                    stream = twitchApi.getStreamByUserId(twitchApi.getStreamerUser().getId());
                } catch (HystrixRuntimeException e) {
                    return "error retrieving stream data";
                }
                if (stream == null) {
                    return "stream is not live";
                } else {
                    return getTimeString(stream.getUptime().toMillis() / 1000);
                }
            }
            case TYPE_URL_FETCH -> {
                return submitRequest(content);
            }
            case TYPE_USER -> {
                if (commandItem == null) {
                    return ERROR_NON_COMMAND;
                }
                return displayName;
            }
            case TYPE_USER_ID -> {
                if (commandItem == null) {
                    return ERROR_NON_COMMAND;
                }
                return userId;
            }
            case TYPE_WEIGHTED -> {
                String[] entries = content.split("\\|");
                int totalWeight = 0;
                NavigableMap<Integer, String> messageMap = new TreeMap<>();
                for (String entry : entries) {
                    String[] weightMessage = entry.split("\\s", 2);
                    if (weightMessage.length != 2) {
                        return ERROR_BAD_ENTRY;
                    }
                
                    int weight;
                    try {
                        weight = Integer.parseInt(weightMessage[0]);
                    } catch (NumberFormatException e) {
                        return ERROR_INVALID_WEIGHT;
                    }
                
                    totalWeight += weight;
                    messageMap.put(totalWeight, weightMessage[1]);
                }
            
                int selection = random.nextInt(totalWeight);
                return messageMap.higherEntry(selection).getValue();
            }
            default -> {
                return ERROR;
            }
        }
    }

    private String getFollowAgeString(String userName) {
        User user;
        try {
            user = twitchApi.getUserByUsername(userName);
        } catch (HystrixRuntimeException e) {
            return String.format("Error retrieving user data for @%s", userName);
        }
        if (user == null) {
            return String.format("Unknown user \"%s\"", userName);
        }
        InboundFollow follow;
        try {
            follow = twitchApi.getChannelFollower(twitchApi.getStreamerUser().getId(), user.getId());
        } catch (HystrixRuntimeException e) {
            return String.format("Error retrieving follow age for %s", userName);
        }

        if (follow == null) {
            return String.format(
                    "%s is not following %s",
                    user.getDisplayName(),
                    twitchApi.getStreamerUser().getDisplayName()
            );
        }
        
        LocalDate followDate = follow.getFollowedAt().atZone(ZoneId.systemDefault()).toLocalDate();
        LocalDate today = LocalDate.now();
        Period period = Period.between(followDate, today);
        int years = period.getYears();
        int months = period.getMonths();
        int days = period.getDays();
        if (years == 0 && months == 0 && days == 0) {
            return String.format(
                    "%s followed %s today",
                    user.getDisplayName(),
                    twitchApi.getStreamerUser().getDisplayName()
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
                twitchApi.getStreamerUser().getDisplayName(),
                timeString
        );
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
        } else if (minutes > 0) {
            return String.format("%d minutes, %d seconds", minutes, seconds);
        } else {
            return String.format("%d seconds", seconds);
        }
    }
}
