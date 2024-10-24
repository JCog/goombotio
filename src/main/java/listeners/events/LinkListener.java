package listeners.events;

import api.bluesky.BlueskyApi;
import api.youtube.YoutubeApi;
import com.github.twitch4j.chat.events.channel.ChannelMessageEvent;
import com.github.twitch4j.helix.domain.Clip;
import com.github.twitch4j.helix.domain.Game;
import com.github.twitch4j.helix.domain.Video;
import com.netflix.hystrix.exception.HystrixRuntimeException;
import listeners.TwitchEventListener;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.v1.Status;
import util.CommonUtils;
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
    private static final Pattern shortPattern = Pattern.compile("(?:www\\.)?youtube\\.com/shorts/([a-zA-Z0-9_\\-]{1,11})");
    private static final Pattern tweetPattern = Pattern.compile("(?:www\\.)?(?:twitter|x)\\.com/[a-zA-Z0-9_]+/status/([0-9]+)");
    private static final Pattern blueskyPattern = Pattern.compile("(?:www\\.)?bsky\\.app/profile/([a-zA-Z0-9_.]+)/post/([a-z0-9]+)");
    
    private final TwitchApi twitchApi;
    private final Twitter twitter;
    private final BlueskyApi blueskyApi;
    private final YoutubeApi youtubeApi;
    private final String youtubeApiKey;

    public LinkListener(CommonUtils commonUtils, Twitter twitter, String youtubeApiKey) {
        twitchApi = commonUtils.twitchApi();
        blueskyApi = commonUtils.apiManager().getBlueskyApi();
        youtubeApi = commonUtils.apiManager().getYoutubeApi();
        this.twitter = twitter;
        this.youtubeApiKey = youtubeApiKey;
    }

    @Override
    public void onChannelMessage(ChannelMessageEvent messageEvent) {
        List<String> clipUrls = getMatches(messageEvent.getMessage(), clipPattern);
        List<String> videoUrls = getMatches(messageEvent.getMessage(), videoPattern);
        List<String> youtubeVideoIds = getMatches(messageEvent.getMessage(), youtubePattern);
        List<String> shortIds = getMatches(messageEvent.getMessage(), shortPattern);
        List<String> tweetIds = getMatches(messageEvent.getMessage(), tweetPattern);
        List<BlueskyMatch> bluskyMatches = getBlueskyMatches(messageEvent.getMessage(), blueskyPattern);

        for (String id : clipUrls) {
            twitchApi.channelMessage(getClipDetails(id));
        }
        for (String id : videoUrls) {
            twitchApi.channelMessage(getVideoDetails(id));
        }
        for (String id : youtubeVideoIds) {
            twitchApi.channelMessage(youtubeApi.getVideoDetails(id, youtubeApiKey, false));
        }
        for (String id : shortIds) {
            twitchApi.channelMessage(youtubeApi.getVideoDetails(id, youtubeApiKey, true));
        }
        for (String id : tweetIds) {
            twitchApi.channelMessage(getTweetDetails(id));
        }
        for (BlueskyMatch match : bluskyMatches) {
            twitchApi.channelMessage(blueskyApi.getPostDetails(match.handle, match.postId));
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
    
    private record BlueskyMatch(String handle, String postId) {}
    
    private List<BlueskyMatch> getBlueskyMatches(String message, Pattern pattern) {
        List<BlueskyMatch> output = new ArrayList<>();
        Matcher matcher = pattern.matcher(message);
        while (matcher.find()) {
            output.add(new BlueskyMatch(matcher.group(1), matcher.group(2)));
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
                "Twitch Clip: %s • %s playing %s • Clipped by %s • %s view%s",
                title,
                channelDisplayName,
                gameName,
                clippedByDisplayName,
                numberFormat.format(viewCount),
                viewCount == 1 ? "" : "s"
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
                "Twitch Video: %s • %s • %s view%s",
                title,
                username,
                numberFormat.format(viewCount),
                viewCount == 1 ? "" : "s"
        );
    }

    private String getTweetDetails(String id) {
        Status tweet;
        try {
            tweet = twitter.v1().tweets().showStatus(Long.parseLong(id));
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
