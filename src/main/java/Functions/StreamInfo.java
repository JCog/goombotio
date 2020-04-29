package Functions;

import com.github.twitch4j.TwitchClient;
import com.github.twitch4j.helix.domain.GameList;
import com.github.twitch4j.helix.domain.Stream;
import com.github.twitch4j.helix.domain.StreamList;
import com.github.twitch4j.helix.domain.UserList;

import java.util.*;
import java.util.concurrent.TimeUnit;

import static java.lang.System.out;

public class StreamInfo {
    
    private static final int timerInterval = 60*1000;
    
    private final String streamer;
    private final TwitchClient twitchClient;
    private final String authToken;
    private final Timer timer = new Timer();
    private final ArrayList<Integer> viewerCounts = new ArrayList<>();
    private final String streamerId;
    private final String streamerDisplayName;

    private Stream streamStats = null;
    private Date startTime = null;
    private Date endTime = null;
    private boolean isLive = false;
    
    
    /**
     * Provides an interface to get basic information about a Twitch stream
     * @param streamer username of the streamer
     * @param twitchClient twitchClient object to query the Twitch API (must have Helix enabled)
     * @param authToken bot's auth token
     */
    public StreamInfo(String streamer, TwitchClient twitchClient, String authToken) {
        this.streamer = streamer;
        this.twitchClient = twitchClient;
        this.authToken = authToken;
        UserList tempList = twitchClient.getHelix().getUsers(authToken, null, Collections.singletonList(streamer)).execute();
        streamerDisplayName = tempList.getUsers().get(0).getDisplayName();
        streamerId = tempList.getUsers().get(0).getId();
    }
    
    /**
     * Starts the thread to get updated info on the stream every minute
     */
    public void startTracker() {

        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                updateStreamStats();
            }
        }, 0, timerInterval);
    }
    
    /**
     * Stops the collection of data on the stream
     */
    public void stopTracker() {
        timer.cancel();
    }
    
    /**
     * Returns true if the stream is live, false otherwise. Note that this may be a few minutes out of date.
     * @return live status of the stream
     */
    public boolean isLive() {
        return isLive;
    }
    
    /**
     * Retrieves the title of the stream. Returns an empty string if the channel isn't live.
     * @return stream title
     */
    public String getTitle() {
        if (isLive()) {
            return streamStats.getTitle();
        }
        return "";
    }
    
    /**
     * Retrieves the name of the game/category the streamer is streaming to. Returns an empty string if the
     * channel isn't live.
     * @return game/category name
     */
    public String getGame() {
        if (isLive()) {
            String gameId = streamStats.getGameId();
            GameList gameList = twitchClient.getHelix().getGames(authToken, Collections.singletonList(gameId), null).execute();
            return gameList.getGames().get(0).getName();
        }
        return "";
    }
    
    /**
     * Returns the average concurrent viewers watching the stream during the current session.
     * @return average viewer count
     */
    public int getAverageViewers() {
        int sum = 0;
        for (Integer count : viewerCounts) {
            sum += count;
        }
        if (viewerCounts.size() == 0) {
            return 0;
        }
        return sum / viewerCounts.size();
    }
    
    /**
     * Returns the median concurrent viewers watching the stream during the current session.
     * @return median viewer count
     */
    public int getMedianViewers() {
        ArrayList<Integer> viewersCounts = new ArrayList<>(viewerCounts);
        Collections.sort(viewersCounts);
        boolean isEven = viewersCounts.size() % 2 == 0;
        int middleIndex = viewersCounts.size() / 2;

        if (viewersCounts.size() == 0) {
            return 0;
        }
        else if (isEven) {
            int first = viewersCounts.get(middleIndex - 1);
            int second = viewersCounts.get(middleIndex);
            return (first + second) / 2;
        }
        else {
            return viewersCounts.get(middleIndex);
        }
    }
    
    /**
     * Returns the maximum concurrent viewers watching the stream during the current session.
     * @return average viewer count
     */
    public int getMaxViewers() {
        int max = 0;
        for (Integer count : viewerCounts) {
            max = Math.max(max, count);
        }
        return max;
    }
    
    public Date getStartTime() {
        return startTime;
    }
    
    public Date getEndTime() {
        return endTime;
    }
    
    public List<Integer> getViewerCounts() {
        return viewerCounts;
    }
    
    /**
     * Returns the time since the start of the stream in minutes
     * @return stream length in minutes
     */
    public int getStreamLength() {
        if (startTime == null) {
            return 0;
        }
        
        Date endTemp = (endTime == null ? new Date() : endTime);
        long duration  = endTemp.getTime() - startTime.getTime();
        return Math.toIntExact(TimeUnit.MILLISECONDS.toMinutes(duration));
    }
    
    public String getChannelName() {
        return streamer;
    }
    
    public String getChannelDisplayName() {
        return streamerDisplayName;
    }
    
    public String getChannelId() {
        return streamerId;
    }
    
    /**
     * Returns uptime in seconds according to Helix
     * @return uptime in seconds, -1 if stream is offline
     */
    public long getUptime() {
        if (isLive()) {
            return streamStats.getUptime().toMillis() / 1000;
        }
        else {
            return -1;
        }
    }

    private void updateStreamStats() {
        StreamList resultList;
        try {
            resultList = twitchClient.getHelix().getStreams(authToken, "", "", 1,
                    null, null, null, null,
                    Collections.singletonList(streamer)).execute();
        }
        catch (Exception e) {
            streamStats = null;
            updateLiveStatus(false);
            return;
        }
        
        if (resultList.getStreams().isEmpty()) {
            streamStats = null;
            updateLiveStatus(false);
        }
        else {
            streamStats = resultList.getStreams().get(0);
            updateViewerCounts();
            updateLiveStatus(true);
        }
    }

    private void updateLiveStatus(boolean isLive) {
        if (isLive != this.isLive) {
            this.isLive = isLive;
            out.println("---------------------");
            if (isLive) {
                setStartTime();
                out.println(streamer + " is now live.");
            }
            else {
                setEndTime();
                out.println(streamer + " has gone offline.");
            }
            out.println("---------------------");
        }
    }

    private void setStartTime() {
        if (startTime == null) {
            startTime = new Date();
        }
    }
    
    private void setEndTime() {
        endTime = new Date();
    }

    private void updateViewerCounts() {
        if (isLive()) {
            int viewers = streamStats.getViewerCount();
            viewerCounts.add(viewers);
        }
    }
}
