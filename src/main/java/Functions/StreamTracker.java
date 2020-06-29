package Functions;

import Listeners.Events.CloudListener;
import Util.ReportBuilder;
import Util.TwirkInterface;
import Util.TwitchApi;
import com.github.twitch4j.helix.domain.Stream;
import com.github.twitch4j.helix.domain.User;
import com.netflix.hystrix.exception.HystrixRuntimeException;

import java.io.File;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;

public class StreamTracker {
    private static final String BLACKLIST_FILENAME = "blacklist.txt";
    private static final int INTERVAL = 60 * 1000;
    
    private final Timer timer = new Timer();
    private final HashSet<String> blacklist = blacklistInit();
    
    private final TwirkInterface twirk;
    private final TwitchApi twitchApi;
    private final User streamerUser;
    private final CloudListener cloudListener;
    
    private StreamData streamData;
    
    public StreamTracker(TwirkInterface twirk, TwitchApi twitchApi, User streamerUser, CloudListener cloudListener) {
        this.twirk = twirk;
        this.twitchApi = twitchApi;
        this.streamerUser = streamerUser;
        this.cloudListener = cloudListener;
        
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
                    HashSet<String> usersOnline = new HashSet<>();
                    for (String user : twirk.getUsersOnline()) {
                        if (!blacklist.contains(user)) {
                            usersOnline.add(user);
                        }
                    }
                    if (usersOnline.isEmpty()) {
                        return;
                    }
                    if (streamData == null) {
                        streamData = new StreamData(twitchApi, streamerUser);
                        cloudListener.reset();
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
    
    public int getViewerMinutes(String username) {
        if (streamData == null) {
            return 0;
        }
        else {
            return streamData.getViewerMinutes(username);
        }
    }
    
    private HashSet<String> blacklistInit() {
        HashSet<String> blacklist = new HashSet<>();
        try {
            File file = new File(BLACKLIST_FILENAME);
            Scanner sc = new Scanner(file);
            while(sc.hasNextLine()) {
                blacklist.add(sc.nextLine());
            }
            sc.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        
        return blacklist;
    }
}
