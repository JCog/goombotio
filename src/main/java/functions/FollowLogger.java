package functions;

import com.github.twitch4j.helix.domain.InboundFollow;
import com.github.twitch4j.helix.domain.User;
import com.netflix.hystrix.exception.HystrixRuntimeException;
import database.DbManager;
import database.stats.WatchTimeDb;
import util.TwitchApi;

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
    private static final String DATE_FORMAT_CURRENT = "yyyy-MM-dd HH:mm:ss";
    private static final String DATE_FORMAT_FOLLOW = "yyyy-MM-dd";
    private static final int INTERVAL = 5; //minutes

    private final WatchTimeDb watchTimeDb;
    private final TwitchApi twitchApi;
    private final StreamTracker streamTracker;
    private final ScheduledExecutorService scheduler;
    private final SimpleDateFormat dateFormatCurrent;
    private final SimpleDateFormat dateFormatFollow;
    
    private Set<String> oldFollowerIdList;
    private PrintWriter writer;
    private ScheduledFuture<?> scheduledFuture;

    public FollowLogger(
            DbManager dbManager,
            TwitchApi twitchApi,
            StreamTracker streamTracker,
            ScheduledExecutorService scheduler
    ) {
        this.twitchApi = twitchApi;
        this.streamTracker = streamTracker;
        this.scheduler = scheduler;
        this.dateFormatCurrent = new SimpleDateFormat(DATE_FORMAT_CURRENT);
        this.dateFormatFollow = new SimpleDateFormat(DATE_FORMAT_FOLLOW);
        watchTimeDb = dbManager.getWatchTimeDb();

        FileWriter fw;
        try {
            fw = new FileWriter(FILENAME, true);
        } catch (IOException e) {
            System.out.printf("ERROR: IOException for filename \"%s\"%n", FILENAME);
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
                Set<String> updatedFollowerIds;
                try {
                    updatedFollowerIds = fetchFollowerIds();
                } catch (HystrixRuntimeException e) {
                    System.out.printf(
                            "Error retrieving updated follower list. Trying again in %d min.%n",
                            INTERVAL
                    );
                    return;
                }
                if (oldFollowerIdList == null) {
                    oldFollowerIdList = updatedFollowerIds;
                    System.out.println("Successfully retrieved initial follower list.");
                    return;
                }
                List<String> newFollowers = getNewFollowers(updatedFollowerIds);
                List<String> unfollowers = getUnfollowers(updatedFollowerIds);
                for (String newFollowerId : newFollowers) {
                    User newFollowerUser;
                    try {
                        newFollowerUser = twitchApi.getUserById(newFollowerId);
                    } catch (HystrixRuntimeException e) {
                        e.printStackTrace();
                        System.out.printf(
                                "error retrieving data for new follower with id %s%n",
                                newFollowerId
                        );
                        newFollowerUser = null;
                    }
                    if (newFollowerUser != null) {
                        writer.write(String.format(
                                "%s New follower: %s - First seen: %s - Watchtime: %d minutes\n",
                                getCurrentDateString(),
                                newFollowerUser.getDisplayName(),
                                prevDateToString(watchTimeDb.getFirstSeenById(newFollowerId)),
                                watchTimeDb.getMinutesById(newFollowerId) + streamTracker.getViewerMinutesById(newFollowerUser.getId())
                        ));
                    } else {
                        String name = watchTimeDb.getNameById(newFollowerId);
                        writer.write(String.format(
                                "%s New follower (invalid state): %s - First seen: %s - Watchtime: %d minutes\n",
                                getCurrentDateString(),
                                name.isEmpty() ? "id: " + newFollowerId : name,
                                prevDateToString(watchTimeDb.getFirstSeenById(newFollowerId)),
                                watchTimeDb.getMinutesById(newFollowerId)
                        ));
                    }
                }
                for (String unfollowerId : unfollowers) {
                    User unfollowerUser;
                    try {
                        unfollowerUser = twitchApi.getUserById(unfollowerId);
                    } catch (HystrixRuntimeException e) {
                        e.printStackTrace();
                        System.out.printf(
                                "error retrieving data for unfollower with id %s%n",
                                unfollowerId
                        );
                        unfollowerUser = null;
                    }
                    if (unfollowerUser != null) {
                        writer.write(String.format(
                                "%s Unfollower: %s - First seen: %s - Last seen: %s - Watchtime: %d minutes\n",
                                getCurrentDateString(),
                                unfollowerUser.getDisplayName(),
                                prevDateToString(watchTimeDb.getFirstSeenById(unfollowerId)),
                                prevDateToString(watchTimeDb.getLastSeenById(unfollowerId)),
                                watchTimeDb.getMinutesById(unfollowerId) + streamTracker.getViewerMinutesById(unfollowerUser.getId())
                        ));
                    } else {
                        String name = watchTimeDb.getNameById(unfollowerId);
                        writer.write(String.format(
                                "%s Unfollower (account deleted): %s - First seen: %s - Last seen: %s - Watchtime: %d minutes\n",
                                getCurrentDateString(),
                                name.isEmpty() ? "id: " + unfollowerId : name,
                                prevDateToString(watchTimeDb.getFirstSeenById(unfollowerId)),
                                prevDateToString(watchTimeDb.getLastSeenById(unfollowerId)),
                                watchTimeDb.getMinutesById(unfollowerId)
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

    private Set<String> fetchFollowerIds() throws HystrixRuntimeException {
        List<InboundFollow> followers = twitchApi.getChannelFollowers(twitchApi.getStreamerUser().getId());
        Set<String> followerIds = new HashSet<>();
        for (InboundFollow follow : followers) {
            followerIds.add(follow.getUserId());
        }
        return followerIds;
    }

    private List<String> getNewFollowers(Set<String> updatedFollowerIdList) {
        List<String> newFollowerIds = new ArrayList<>();
        for (String followerId : updatedFollowerIdList) {
            if (!oldFollowerIdList.contains(followerId)) {
                newFollowerIds.add(followerId);
            }
        }
        return newFollowerIds;
    }

    private List<String> getUnfollowers(Set<String> updatedFollowerIdList) {
        List<String> unfollowerIds = new ArrayList<>();
        for (String followerId : oldFollowerIdList) {
            if (!updatedFollowerIdList.contains(followerId)) {
                unfollowerIds.add(followerId);
            }
        }
        return unfollowerIds;
    }

    private String getCurrentDateString() {
        return dateFormatCurrent.format(new Date());
    }

    private String prevDateToString(Date date) {
        if (date == null) {
            return "never";
        }
        return dateFormatFollow.format(date);
    }
}
