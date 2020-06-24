package Functions;

import Database.Stats.WatchTimeDb;
import Util.TwitchApi;
import com.github.twitch4j.helix.domain.Follow;
import com.github.twitch4j.helix.domain.User;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.*;

public class FollowLogger {
    private static final String FILENAME = "follows.log";
    private static final String TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    private static final int INTERVAL = 5 * 60 * 1000; //5 minutes
    
    private final Timer timer = new Timer();
    private final WatchTimeDb watchTimeDb = WatchTimeDb.getInstance();
    private final TwitchApi twitchApi;
    private final User streamerUser;
    
    private HashSet<String> oldFollowerIdList;
    private PrintWriter writer;
    
    public FollowLogger(TwitchApi twitchApi, User streamerUser) {
        this.twitchApi = twitchApi;
        this.streamerUser = streamerUser;
        oldFollowerIdList = fetchFollowerIds();
        
        FileWriter fw;
        try {
            fw = new FileWriter(FILENAME, true);
        }
        catch (IOException e) {
            System.out.println(String.format("ERROR: IOException for filename \"%s\"", FILENAME));
            e.printStackTrace();
            return;
        }
        BufferedWriter bw = new BufferedWriter(fw);
        writer = new PrintWriter(bw);
    }
    
    public void start() {
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                HashSet<String> updatedFollowerIds = fetchFollowerIds();
                ArrayList<String> newFollowers = getNewFollowers(updatedFollowerIds);
                ArrayList<String> unfollowers = getUnfollowers(updatedFollowerIds);
                for (String newFollowerId : newFollowers) {
                    User newFollowerUser = twitchApi.getUserById(newFollowerId);
                    if (newFollowerUser != null) {
                        writer.write(String.format(
                                "%s New follower: %s - First seen: %s - Watchtime: %d minutes\n",
                                getDateString(),
                                newFollowerUser.getDisplayName(),
                                watchTimeDb.getFirstSeen(newFollowerId).toString(),
                                watchTimeDb.getMinutes(newFollowerId)
                        ));
                    }
                    else {
                        String name = watchTimeDb.getName(newFollowerId);
                        writer.write(String.format(
                                "%s New follower (invalid state): %s - First seen: %s - Watchtime: %d minutes\n",
                                getDateString(),
                                name.isEmpty() ? "id: " + newFollowerId : name,
                                watchTimeDb.getFirstSeen(newFollowerId),
                                watchTimeDb.getMinutes(newFollowerId)
                        ));
                    }
                }
                for (String unfollowerId : unfollowers) {
                    User unfollowerUser = twitchApi.getUserById(unfollowerId);
                    if (unfollowerUser != null) {
                        writer.write(String.format(
                                "%s Unfollower: %s - First seen: %s - Last seen: %s - Watchtime: %d minutes\n",
                                getDateString(),
                                unfollowerUser.getDisplayName(),
                                watchTimeDb.getFirstSeen(unfollowerId).toString(),
                                watchTimeDb.getLastSeen(unfollowerId).toString(),
                                watchTimeDb.getMinutes(unfollowerId)
                        ));
                    }
                    else {
                        String name = watchTimeDb.getName(unfollowerId);
                        writer.write(String.format(
                                "%s Unfollower (account deleted): %s - First seen: %s - Last seen: %s - Watchtime: %d minutes\n",
                                getDateString(),
                                name.isEmpty() ? "id: " + unfollowerId : name,
                                watchTimeDb.getFirstSeen(unfollowerId),
                                watchTimeDb.getLastSeen(unfollowerId),
                                watchTimeDb.getMinutes(unfollowerId)
                        ));
                    }
                }
                writer.flush();
                oldFollowerIdList = updatedFollowerIds;
            }
        }, 0, INTERVAL);
    }
    
    public void stop() {
        timer.cancel();
        writer.close();
    }
    
    private HashSet<String> fetchFollowerIds() {
        List<Follow> followers = twitchApi.getFollowers(streamerUser.getId());
        HashSet<String> followerIds = new HashSet<>();
        for (Follow follow : followers) {
            followerIds.add(follow.getFromId());
        }
        return followerIds;
    }
    
    private ArrayList<String> getNewFollowers(HashSet<String> updatedFollowerIdList) {
        ArrayList<String> newFollowerIds = new ArrayList<>();
        for (String followerId : updatedFollowerIdList) {
            if (!oldFollowerIdList.contains(followerId)) {
                newFollowerIds.add(followerId);
            }
        }
        return newFollowerIds;
    }
    
    private ArrayList<String> getUnfollowers(HashSet<String> updatedFollowerIdList) {
        ArrayList<String> unfollowerIds = new ArrayList<>();
        for (String followerId : oldFollowerIdList) {
            if (!updatedFollowerIdList.contains(followerId)) {
                unfollowerIds.add(followerId);
            }
        }
        return unfollowerIds;
    }
    
    private String getDateString() {
        SimpleDateFormat sdf = new SimpleDateFormat(TIME_FORMAT);
        return sdf.format(new Date());
    }
}
