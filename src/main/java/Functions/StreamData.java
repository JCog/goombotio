package Functions;

import Database.Stats.StreamStatsDb;
import Database.Stats.WatchTimeDb;
import Util.Settings;
import Util.TwitchApi;
import com.github.twitch4j.helix.domain.User;

import java.io.File;
import java.util.*;

import static java.lang.System.out;

class StreamData {
    private static final String BLACKLIST_FILENAME = "src/main/resources/blacklist.txt";
    
    private final HashMap<String, Integer> userMinutes = new HashMap<>();
    private final ArrayList<Integer> viewerCounts = new ArrayList<>();
    private final String streamer = Settings.getTwitchStream();
    private final StreamStatsDb streamStatsDb = StreamStatsDb.getInstance();
    private final WatchTimeDb watchTimeDb = WatchTimeDb.getInstance();
    private final ArrayList<String> blacklist = blacklistInit(BLACKLIST_FILENAME);
    
    private final TwitchApi twitchApi;
    private final Date startTime;
    
    private Date endTime;
    
    public StreamData(TwitchApi twitchApi) {
        this.twitchApi = twitchApi;
        
        out.println("---------------------");
        out.println(streamer + " is now live.");
        out.println("---------------------");
        startTime = new Date();
    }
    
    public void updateUsersMinutes(Collection<String> userList) {
        for (String user : userList) {
            userMinutes.putIfAbsent(user, 0);
            userMinutes.put(user, userMinutes.get(user) + 1);
        }
    }
    
    public void updateViewerCounts(int viewCount) {
        viewerCounts.add(viewCount);
    }
    
    public void endStream() {
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
}
