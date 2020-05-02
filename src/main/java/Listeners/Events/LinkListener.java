package Listeners.Events;

import APIs.YoutubeApi;
import Util.TwirkInterface;
import com.gikk.twirk.events.TwirkListener;
import com.gikk.twirk.types.twitchMessage.TwitchMessage;
import com.gikk.twirk.types.users.TwitchUser;
import com.github.twitch4j.TwitchClient;
import com.github.twitch4j.helix.domain.*;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LinkListener implements TwirkListener {
    private static final Pattern clipPattern = Pattern.compile("(?:https?://www\\.|www\\.|^|\\s)+twitch\\.tv/[a-zA-Z0-9_]+/clip/([a-zA-Z]+)");
    private static final Pattern videoPattern = Pattern.compile("(?:https?://www\\.|www\\.|^|\\s)+twitch\\.tv/videos/([0-9]+)");
    private static final Pattern youtubePattern = Pattern.compile("(?:https?://|^|\\s)+(?:www\\.)?(?:youtube\\.com/watch\\?v=|youtu\\.be/)([a-zA-Z0-9_\\-]+)");
    
    private final TwirkInterface twirk;
    private final TwitchClient twitchClient;
    private final String authToken;
    private final String youtubeApiKey;
    
    public LinkListener(TwirkInterface twirk, TwitchClient twitchClient, String authToken, String youtubeApiKey) {
        this.twirk = twirk;
        this.twitchClient = twitchClient;
        this.authToken = authToken;
        this.youtubeApiKey = youtubeApiKey;
    }
    
    @Override
    public void onPrivMsg(TwitchUser sender, TwitchMessage message) {
        ArrayList<String> clipUrls = getUrls(message.getContent(), clipPattern);
        ArrayList<String> videoUrls = getUrls(message.getContent(), videoPattern);
        ArrayList<String> youtubeVideoIds = getUrls(message.getContent(), youtubePattern);
        
        for (String id : clipUrls) {
            twirk.channelMessage(getClipDetails(id));
        }
        for (String id : videoUrls) {
            twirk.channelMessage(getVideoDetails(id));
        }
        for (String id : youtubeVideoIds) {
            twirk.channelMessage(YoutubeApi.getVideoDetails(id, youtubeApiKey));
        }
    }
    
    private ArrayList<String> getUrls(String message, Pattern pattern) {
        ArrayList<String> output = new ArrayList<>();
        Matcher matcher = pattern.matcher(message);
        while (matcher.find()) {
            output.add(matcher.group(1));
        }
        return output;
    }
    
    private String getClipDetails(String id) {
        ClipList clipList = twitchClient.getHelix().getClips(
                authToken,
                null,
                null,
                id,
                null,
                null,
                1,
                null,
                null
        ).execute();
        
        Clip clip;
        try {
            clip = clipList.getData().get(0);
        }
        catch (IndexOutOfBoundsException e) {
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
        
        UserList userList = twitchClient.getHelix().getUsers(authToken, userIds, null).execute();
        String channelDisplayName = userList.getUsers().get(0).getDisplayName();
        String clippedByDisplayName = userList.getUsers().get(1).getDisplayName();
        
        GameList gameList = twitchClient.getHelix().getGames(authToken, Collections.singletonList(gameId), null).execute();
        String gameName = gameList.getGames().get(0).getName();
        
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
        VideoList videoList = twitchClient.getHelix().getVideos(
                authToken,
                id,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                1
        ).execute();
        
        Video video;
        try {
            video = videoList.getVideos().get(0);
        }
        catch (IndexOutOfBoundsException e) {
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
}
