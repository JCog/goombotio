package Functions;

import com.github.twitch4j.TwitchClient;
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

    public StreamInfo(String streamer, TwitchClient twitchClient) {
        this.streamer = streamer;
        this.twitchClient = twitchClient;
        timer = new Timer();
        timerInterval = 60*1000;
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
        return streamStats != null;
    }

    public String getTitle() {
        if (isLive()) {
            return streamStats.getTitle();
        }
        return "";
    }

    private void updateStreamStats() {
        StreamList resultList = twitchClient.getHelix().getStreams("", "", "", 1,
                null, null, null, null,
                Collections.singletonList(streamer)).execute();
        if (resultList.getStreams().isEmpty()) {
            streamStats = null;
            out.println("stream is not live");
        }
        else {
            streamStats = resultList.getStreams().get(0);
            out.println("stream is live");
        }
    }
}
