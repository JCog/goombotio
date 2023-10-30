package listeners.events;

import api.YoutubeApi;
import com.github.twitch4j.chat.events.channel.ChannelMessageEvent;
import com.github.twitch4j.helix.domain.Clip;
import com.github.twitch4j.helix.domain.Game;
import com.github.twitch4j.helix.domain.Video;
import com.netflix.hystrix.exception.HystrixRuntimeException;
import listeners.TwitchEventListener;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import util.TwitchApi;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LinkListener implements TwitchEventListener {
    private static final Pattern clipPattern = Pattern.compile("(?:www\\.|clips\\.)?twitch\\.tv/(?:[a-zA-Z0-9_]+/clip/)?([a-zA-Z0-9-_]+)");
    private static final Pattern videoPattern = Pattern.compile("(?:www\\.)?twitch\\.tv/videos/([0-9]+)");
    private static final Pattern youtubePattern = Pattern.compile("(?:www\\.)?(?:youtube\\.com/watch\\?[a-zA-Z0-9_=&]*v=|youtu\\.be/)([a-zA-Z0-9_\\-]{1,11})");
    private static final Pattern tweetPattern = Pattern.compile("(?:www\\.)?(?:twitter|x)\\.com/[a-zA-Z0-9_]+/status/([0-9]+)");

    private final TwitchApi twitchApi;
    private final Twitter twitter;
    private final String youtubeApiKey;

    public LinkListener(TwitchApi twitchApi, Twitter twitter, String youtubeApiKey) {
        this.twitchApi = twitchApi;
        this.youtubeApiKey = youtubeApiKey;
        this.twitter = twitter;
    }

    @Override
    public void onChannelMessage(ChannelMessageEvent messageEvent) {
        List<String> clipUrls = getMatches(messageEvent.getMessage(), clipPattern);
        List<String> videoUrls = getMatches(messageEvent.getMessage(), videoPattern);
        List<String> youtubeVideoIds = getMatches(messageEvent.getMessage(), youtubePattern);
        List<String> tweetIds = getMatches(messageEvent.getMessage(), tweetPattern);

        for (String id : clipUrls) {
            twitchApi.channelMessage(getClipDetails(id));
        }
        for (String id : videoUrls) {
            twitchApi.channelMessage(getVideoDetails(id));
        }
        for (String id : youtubeVideoIds) {
            twitchApi.channelMessage(YoutubeApi.getVideoDetails(id, youtubeApiKey));
        }
        for (String id : tweetIds) {
            twitchApi.channelMessage(getTweetDetails(id));
        }
    }

    private List<String> getMatches(String message, Pattern pattern) {
        List<String> output = new ArrayList<>();
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
        } catch (HystrixRuntimeException e) {
            e.printStackTrace();
            return "Error retrieving clip data";
        }
        if (clip == null) {
            return "";
        }

        String title = clip.getTitle();
        int viewCount = clip.getViewCount();
        String gameId = clip.getGameId();
        String channelDisplayName = clip.getBroadcasterName();
        String clippedByDisplayName = clip.getCreatorName();

        Game game;
        try {
            game = twitchApi.getGameById(gameId);
        } catch (HystrixRuntimeException e) {
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
        } catch (HystrixRuntimeException e) {
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
        } catch (TwitterException e) {
            System.out.println("Twitter Exception");
            return "";
        }

        String user = tweet.getUser().getScreenName();
        String content = tweet.getText().split(" https")[0];
        int retweets = tweet.getRetweetCount();
        int likes = tweet.getFavoriteCount();
    
        NumberFormat numberFormat = NumberFormat.getInstance();
        numberFormat.setGroupingUsed(true);
        return String.format(
                "Tweet by @%s: %s • \uD83D\uDD01%s | ❤%s",
                user,
                content.replaceAll("\\n", " "),
                numberFormat.format(retweets),
                numberFormat.format(likes)
        );
    }
}
