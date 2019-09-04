package Functions;

import com.github.twitch4j.TwitchClient;
import com.github.twitch4j.helix.domain.GameList;
import com.github.twitch4j.helix.domain.Stream;
import com.github.twitch4j.helix.domain.StreamList;

import java.util.Collections;
import java.util.Timer;
import java.util.TimerTask;

import static java.lang.System.out;

public class StreamInfo {

    private String streamer;
    private TwitchClient twitchClient;
    private Timer timer;
    private int timerInterval;
    private Stream streamStats;
    private boolean isLive;
    private String authToken;

    public StreamInfo(String streamer, TwitchClient twitchClient, String authToken) {
        this.streamer = streamer;
        this.twitchClient = twitchClient;
        this.authToken = authToken;
        timer = new Timer();
        timerInterval = 60*1000;
        isLive = false;
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
            updateLiveStatus(true);
        }
    }

    private void updateLiveStatus(boolean isLive) {
        if (isLive != this.isLive) {
            this.isLive = isLive;
            out.println("---------------------");
            if (isLive) {
                out.println(streamer + " is now live.");
            }
            else {
                out.println(streamer + " has gone offline.");
            }
            out.println("---------------------");
        }
    }
}
