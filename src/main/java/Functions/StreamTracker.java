package Functions;

import Database.Stats.StreamStatsDb;
import Database.Stats.WatchTimeDb;
import Util.Settings;
import Util.TwirkInterface;
import Util.TwitchApi;
import com.github.twitch4j.helix.domain.Stream;
import com.github.twitch4j.helix.domain.User;

import java.io.File;
import java.util.*;

import static java.lang.System.out;

public class StreamTracker {
    private static final String BLACKLIST_FILENAME = "src/main/resources/blacklist.txt";
    private static final int INTERVAL = 60 * 1000;
    
    private final StreamStatsDb streamStatsDb = StreamStatsDb.getInstance();
    private final WatchTimeDb watchTimeDb = WatchTimeDb.getInstance();
    private final ArrayList<String> blacklist = blacklistInit(BLACKLIST_FILENAME);
    private final Timer timer = new Timer();
    
    private final TwirkInterface twirk;
    private final TwitchApi twitchApi;
    private final String streamer;
    
    private StreamData streamData;
    
    public StreamTracker(TwirkInterface twirk, TwitchApi twitchApi) {
        this.twirk = twirk;
        this.twitchApi = twitchApi;
        
        streamer = Settings.getTwitchStream();
        streamData = null;
    }
    
    public void start() {
        
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                Stream stream = twitchApi.getStream();
                if (stream != null) {
                    if (streamData == null) {
                        streamData = new StreamData();
                    }
                    Set<String> usersOnline = twirk.getUsersOnline();
                    usersOnline.forEach(user -> user = user.trim());
                    streamData.updateUsersMinutes(twirk.getUsersOnline());
                    streamData.updateViewerCounts(stream.getViewerCount());
                }
                else {
                    if (streamData != null) {
                        streamData.endStream();
                        streamData = null;
                    }
                }
            }
        }, 0, INTERVAL);
    }
    
    public void stop() {
        if (streamData != null) {
            streamData.endStream();
        }
        timer.cancel();
    }
    
    private ArrayList<String> blacklistInit(String filename) {
        ArrayList<String> blacklist = new ArrayList<>();
        try {
            File file = new File(filename);
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
    
    private class StreamData {
        private final HashMap<String, Integer> userMinutes = new HashMap<>();
        private final ArrayList<Integer> viewerCounts = new ArrayList<>();
        private final Date startTime;
        
        private Date endTime;
        
        private StreamData() {
            out.println("---------------------");
            out.println(streamer + " is now live.");
            out.println("---------------------");
            startTime = new Date();
        }
        
        private void updateUsersMinutes(Collection<String> userList) {
            for (String user : userList) {
                userMinutes.putIfAbsent(user, 0);
                userMinutes.put(user, userMinutes.get(user) + 1);
            }
        }
        
        private void updateViewerCounts(int viewCount) {
            viewerCounts.add(viewCount);
        }
        
        private void endStream() {
            out.println("---------------------");
            out.println(streamer + " has gone offline.");
            out.println("---------------------");
            endTime = new Date();
            
            List<User> userList = twitchApi.getUserListByUsernames(userMinutes.keySet());
            for (User user : userList) {
                if (!blacklist.contains(user.getLogin())) {
                    int minutes = userMinutes.get(user.getLogin());
                    watchTimeDb.addMinutes(user.getId(), user.getLogin(), minutes);
                }
            }
            
            streamStatsDb.addStream(startTime, endTime, viewerCounts, userMinutes);
        }
    }
}
