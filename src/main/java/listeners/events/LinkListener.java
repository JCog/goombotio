package listeners.events;

import api.YoutubeApi;
import com.gikk.twirk.events.TwirkListener;
import com.gikk.twirk.types.twitchMessage.TwitchMessage;
import com.gikk.twirk.types.users.TwitchUser;
import com.github.twitch4j.helix.domain.Clip;
import com.github.twitch4j.helix.domain.Game;
import com.github.twitch4j.helix.domain.User;
import com.github.twitch4j.helix.domain.Video;
import com.netflix.hystrix.exception.HystrixRuntimeException;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import util.TwirkInterface;
import util.TwitchApi;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LinkListener implements TwirkListener {
    private static final Pattern clipPattern = Pattern.compile("(?:www\\.|clips\\.)?twitch\\.tv/(?:[a-zA-Z0-9_]+/clip/)?([a-zA-Z0-9-_]+)");
    private static final Pattern videoPattern = Pattern.compile("(?:www\\.)?twitch\\.tv/videos/([0-9]+)");
    private static final Pattern youtubePattern = Pattern.compile("(?:www\\.)?(?:youtube\\.com/watch\\?[a-zA-Z0-9_=&]*v=|youtu\\.be/)([a-zA-Z0-9_\\-]+)");
    private static final Pattern tweetPattern = Pattern.compile("(?:www\\.)?(?:twitter\\.com/[a-zA-Z0-9_]+/status/)([0-9]+)");

    private final TwirkInterface twirk;
    private final TwitchApi twitchApi;
    private final Twitter twitter;
    private final String youtubeApiKey;

    public LinkListener(TwirkInterface twirk, TwitchApi twitchApi, Twitter twitter, String youtubeApiKey) {
        this.twirk = twirk;
        this.twitchApi = twitchApi;
        this.youtubeApiKey = youtubeApiKey;
        this.twitter = twitter;
    }

    @Override
    public void onPrivMsg(TwitchUser sender, TwitchMessage message) {
        ArrayList<String> clipUrls = getMatches(message.getContent(), clipPattern);
        ArrayList<String> videoUrls = getMatches(message.getContent(), videoPattern);
        ArrayList<String> youtubeVideoIds = getMatches(message.getContent(), youtubePattern);
        ArrayList<String> tweetIds = getMatches(message.getContent(), tweetPattern);

        for (String id : clipUrls) {
            twirk.channelMessage(getClipDetails(id));
        }
        for (String id : videoUrls) {
            twirk.channelMessage(getVideoDetails(id));
        }
        for (String id : youtubeVideoIds) {
            twirk.channelMessage(YoutubeApi.getVideoDetails(id, youtubeApiKey));
        }
        for (String id : tweetIds) {
            twirk.channelMessage(getTweetDetails(id));
        }
    }

    private ArrayList<String> getMatches(String message, Pattern pattern) {
        ArrayList<String> output = new ArrayList<>();
        Matcher matcher = pattern.matcher(message);
        while (matcher.find()) {
            output.add(matcher.group(1));
        }
        return output;
    }

    private String getClipDetails(String id) {
        Clip clip;
        try {
            clip = twitchApi.getClipById(id);
        }
        catch (HystrixRuntimeException e) {
            e.printStackTrace();
            return "Error retrieving clip data";
        }
        if (clip == null) {
            return "";
        }

        String title = clip.getTitle();
        int viewCount = clip.getViewCount();
        String channelId = clip.getBroadcasterId();
        String clippedById = clip.getCreatorId();
        String gameId = clip.getGameId();
        ArrayList<String> userIds = new ArrayList<String>() {
            {
                add(channelId);
                add(clippedById);
            }
        };

        List<User> userList;
        try {
            userList = twitchApi.getUserListByIds(userIds);
        }
        catch (HystrixRuntimeException e) {
            e.printStackTrace();
            return "Error retrieving user data";
        }
        String channelDisplayName;
        String clippedByDisplayName;
        switch (userList.size()) {
            case 2:
                if (userList.get(0).getId().equals(channelId)) {
                    channelDisplayName = userList.get(0).getDisplayName();
                    clippedByDisplayName = userList.get(1).getDisplayName();
                }
                else {
                    channelDisplayName = userList.get(1).getDisplayName();
                    clippedByDisplayName = userList.get(0).getDisplayName();
                }
                break;
            case 1:
                if (channelId.equals(clippedById)) {
                    channelDisplayName = userList.get(0).getDisplayName();
                    clippedByDisplayName = channelDisplayName;
                    break;
                }
                else {
                    return "";
                }
            default:
                return "";
        }

        Game game;
        try {
            game = twitchApi.getGameById(gameId);
        }
        catch (HystrixRuntimeException e) {
            e.printStackTrace();
            return "Error retrieving game data";
        }
        if (game == null) {
            return "";
        }
        String gameName = game.getName();

        NumberFormat numberFormat = NumberFormat.getInstance();
        numberFormat.setGroupingUsed(true);

        return String.format(
                "Twitch Clip: %s • %s playing %s • Clipped by %s • %s views",
                title,
                channelDisplayName,
                gameName,
                clippedByDisplayName,
                numberFormat.format(viewCount)
        );
    }

    private String getVideoDetails(String id) {
        Video video;
        try {
            video = twitchApi.getVideoById(id);
        }
        catch (HystrixRuntimeException e) {
            e.printStackTrace();
            return "Error retrieving video data";
        }
        if (video == null) {
            return "";
        }

        String title = video.getTitle();
        String username = video.getUserName();
        int viewCount = video.getViewCount();

        NumberFormat numberFormat = NumberFormat.getInstance();
        numberFormat.setGroupingUsed(true);

        return String.format(
                "Twitch Video: %s • %s • %s views",
                title,
                username,
                numberFormat.format(viewCount)
        );
    }

    private String getTweetDetails(String id) {
        Status tweet;
        try {
            tweet = twitter.tweets().showStatus(Long.parseLong(id));
        }
        catch (TwitterException e) {
            System.out.println("Twitter Exception");
            return "";
        }

        String user = tweet.getUser().getScreenName();
        String content = tweet.getText().split(" https")[0];
        int retweets = tweet.getRetweetCount();
        int likes = tweet.getFavoriteCount();
        return String.format(
                "Tweet by @%s: %s • \uD83D\uDD01%d | ❤%d",
                user,
                content,
                retweets,
                likes
        );
    }
}
