package Functions;

import Util.ReportBuilder;
import Util.TwirkInterface;
import Util.TwitchApi;
import com.github.twitch4j.helix.domain.Stream;
import com.github.twitch4j.helix.domain.User;
import com.netflix.hystrix.exception.HystrixRuntimeException;

import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

public class StreamTracker {
    private static final int INTERVAL = 60 * 1000;
    
    private final Timer timer = new Timer();
    
    private final TwirkInterface twirk;
    private final TwitchApi twitchApi;
    private final User streamerUser;
    
    private StreamData streamData;
    
    public StreamTracker(TwirkInterface twirk, TwitchApi twitchApi, User streamerUser) {
        this.twirk = twirk;
        this.twitchApi = twitchApi;
        this.streamerUser = streamerUser;
        
        streamData = null;
    }
    
    public void start() {
        
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                Stream stream;
                try {
                    stream = twitchApi.getStream();
                }
                catch (HystrixRuntimeException e) {
                    e.printStackTrace();
                    System.out.println("Error retrieving stream for StreamTracker, skipping interval");
                    return;
                }
                if (stream != null) {
                    Set<String> usersOnline = twirk.getUsersOnline();
                    if (usersOnline == null) {
                        return;
                    }
                    if (streamData == null) {
                        streamData = new StreamData(twitchApi, streamerUser);
                    }
                    streamData.updateUsersMinutes(usersOnline);
                    streamData.updateViewerCounts(stream.getViewerCount());
                }
                else {
                    if (streamData != null) {
                        streamData.endStream();
                        ReportBuilder.generateReport(streamData);
                        streamData = null;
                    }
                }
            }
        }, 0, INTERVAL);
    }
    
    public void stop() {
        timer.cancel();
        if (streamData != null) {
            streamData.endStream();
            ReportBuilder.generateReport(streamData);
            streamData = null;
        }
    }
    
}
