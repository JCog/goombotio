package functions;

import com.github.twitch4j.helix.domain.Follow;
import com.github.twitch4j.helix.domain.User;
import com.jcog.utils.TwitchApi;
import com.jcog.utils.database.DbManager;
import com.jcog.utils.database.stats.WatchTimeDb;
import com.netflix.hystrix.exception.HystrixRuntimeException;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class FollowLogger {
    private static final String FILENAME = "follows.log";
    private static final String TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    private static final int INTERVAL = 5; //minutes

    private final WatchTimeDb watchTimeDb;
    private final TwitchApi twitchApi;
    private final StreamTracker streamTracker;
    private final User streamerUser;
    private final ScheduledExecutorService scheduler;

    private HashSet<String> oldFollowerIdList;
    private PrintWriter writer;
    private ScheduledFuture<?> scheduledFuture;

    public FollowLogger(
            DbManager dbManager,
            TwitchApi twitchApi,
            StreamTracker streamTracker,
            User streamerUser,
            ScheduledExecutorService scheduler
    ) {
        this.twitchApi = twitchApi;
        this.streamTracker = streamTracker;
        this.streamerUser = streamerUser;
        this.scheduler = scheduler;
        watchTimeDb = dbManager.getWatchTimeDb();
        try {
            oldFollowerIdList = fetchFollowerIds();
        }
        catch (HystrixRuntimeException e) {
            e.printStackTrace();
            System.out.println(String.format("Error retrieving initial follower list. Trying again in %dmin", INTERVAL));
            oldFollowerIdList = null;
        }

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
        scheduledFuture = scheduler.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                HashSet<String> updatedFollowerIds;
                try {
                    updatedFollowerIds = fetchFollowerIds();
                }
                catch (HystrixRuntimeException e) {
                    e.printStackTrace();
                    System.out.println(String.format("Error retrieving updated follower list. Trying again in %dmin", INTERVAL));
                    return;
                }
                if (oldFollowerIdList == null) {
                    oldFollowerIdList = updatedFollowerIds;
                    System.out.println("Successfully retrieved initial follower list");
                    return;
                }
                ArrayList<String> newFollowers = getNewFollowers(updatedFollowerIds);
                ArrayList<String> unfollowers = getUnfollowers(updatedFollowerIds);
                for (String newFollowerId : newFollowers) {
                    User newFollowerUser;
                    try {
                        newFollowerUser = twitchApi.getUserById(newFollowerId);
                    }
                    catch (HystrixRuntimeException e) {
                        e.printStackTrace();
                        System.out.println(String.format("error retrieving data for new follower with id %s", newFollowerId));
                        newFollowerUser = null;
                    }
                    long idLong = Long.parseLong(newFollowerId);
                    if (newFollowerUser != null) {
                        writer.write(String.format(
                                "%s New follower: %s - First seen: %s - Watchtime: %d minutes\n",
                                getDateString(),
                                newFollowerUser.getDisplayName(),
                                watchTimeDb.getFirstSeenById(idLong).toString(),
                                watchTimeDb.getMinutesById(idLong) + streamTracker.getViewerMinutes(newFollowerUser.getLogin())
                        ));
                    }
                    else {
                        String name = watchTimeDb.getNameById(idLong);
                        writer.write(String.format(
                                "%s New follower (invalid state): %s - First seen: %s - Watchtime: %d minutes\n",
                                getDateString(),
                                name.isEmpty() ? "id: " + newFollowerId : name,
                                watchTimeDb.getFirstSeenById(idLong).toString(),
                                watchTimeDb.getMinutesById(idLong)
                        ));
                    }
                }
                for (String unfollowerId : unfollowers) {
                    User unfollowerUser;
                    try {
                        unfollowerUser = twitchApi.getUserById(unfollowerId);
                    }
                    catch (HystrixRuntimeException e) {
                        e.printStackTrace();
                        System.out.println(String.format("error retrieving data for unfollower with id %s", unfollowerId));
                        unfollowerUser = null;
                    }
                    long idLong = Long.parseLong(unfollowerId);
                    if (unfollowerUser != null) {
                        writer.write(String.format(
                                "%s Unfollower: %s - First seen: %s - Last seen: %s - Watchtime: %d minutes\n",
                                getDateString(),
                                unfollowerUser.getDisplayName(),
                                watchTimeDb.getFirstSeenById(idLong).toString(),
                                watchTimeDb.getLastSeenById(idLong).toString(),
                                watchTimeDb.getMinutesById(idLong) + streamTracker.getViewerMinutes(unfollowerUser.getLogin())
                        ));
                    }
                    else {
                        String name = watchTimeDb.getNameById(idLong);
                        writer.write(String.format(
                                "%s Unfollower (account deleted): %s - First seen: %s - Last seen: %s - Watchtime: %d minutes\n",
                                getDateString(),
                                name.isEmpty() ? "id: " + unfollowerId : name,
                                watchTimeDb.getFirstSeenById(idLong).toString(),
                                watchTimeDb.getLastSeenById(idLong).toString(),
                                watchTimeDb.getMinutesById(idLong)
                        ));
                    }
                }
                writer.flush();
                oldFollowerIdList = updatedFollowerIds;
            }
        }, 0, INTERVAL, TimeUnit.MINUTES);
    }

    public void stop() {
        scheduledFuture.cancel(false);
        writer.close();
    }

    private HashSet<String> fetchFollowerIds() throws HystrixRuntimeException {
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
