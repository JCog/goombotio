package Functions;

import com.github.twitch4j.TwitchClient;
import com.github.twitch4j.helix.domain.GameList;
import com.github.twitch4j.helix.domain.Stream;
import com.github.twitch4j.helix.domain.StreamList;

import java.util.*;

import static java.lang.System.out;

public class StreamInfo {

    private String streamer;
    private TwitchClient twitchClient;
    private String authToken;

    private Stream streamStats = null;
    private Timer timer = new Timer();
    private int timerInterval = 60*1000;
    private boolean isLive = false;
    private Date startTime = null;
    private ArrayList<Integer> viewerCounts = new ArrayList<>();


    public StreamInfo(String streamer, TwitchClient twitchClient, String authToken) {
        this.streamer = streamer;
        this.twitchClient = twitchClient;
        this.authToken = authToken;
    }

    public void startTracker() {

        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                updateStreamStats();
            }
        }, 0, timerInterval);
    }

    public void stopTracker() {
        timer.cancel();
    }

    public boolean isLive() {
        return isLive;
    }

    public String getTitle() {
        if (isLive()) {
            return streamStats.getTitle();
        }
        return "";
    }

    public String getGame() {
        if (isLive()) {
            String gameId = streamStats.getGameId().toString();
            GameList gameList = twitchClient.getHelix().getGames(authToken, Collections.singletonList(gameId), null).execute();
            return gameList.getGames().get(0).getName();
        }
        return "";
    }

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

    public int getMaxViewers() {
        int max = 0;
        for (Integer count : viewerCounts) {
            max = Math.max(max, count);
        }
        return max;
    }

    private void updateStreamStats() {
        StreamList resultList = twitchClient.getHelix().getStreams(authToken, "", "", 1,
                null, null, null, null,
                Collections.singletonList(streamer)).execute();
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

    private void updateViewerCounts() {
        if (isLive()) {
            int viewers = streamStats.getViewerCount();
            viewerCounts.add(viewers);
        }
    }
}
