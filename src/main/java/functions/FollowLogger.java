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
import java.util.concurrent.TimeUnit;

public class FollowLogger {
    private static final String FILENAME = "follows.log";
    private static final String DATE_FORMAT_CURRENT = "yyyy-MM-dd HH:mm:ss";
    private static final String DATE_FORMAT_FOLLOW = "yyyy-MM-dd";
    private static final int INTERVAL = 5; //minutes
    private static final int MAX_USERNAME_LENGTH = 25;

    private final WatchTimeDb watchTimeDb;
    private final TwitchApi twitchApi;
    private final StreamTracker streamTracker;
    private final SimpleDateFormat dateFormatCurrent;
    private final SimpleDateFormat dateFormatFollow;
    
    private Set<String> oldFollowerIdList;

    public FollowLogger(
            DbManager dbManager,
            TwitchApi twitchApi,
            StreamTracker streamTracker,
            ScheduledExecutorService scheduler
    ) {
        this.twitchApi = twitchApi;
        this.streamTracker = streamTracker;
        this.dateFormatCurrent = new SimpleDateFormat(DATE_FORMAT_CURRENT);
        this.dateFormatFollow = new SimpleDateFormat(DATE_FORMAT_FOLLOW);
        watchTimeDb = dbManager.getWatchTimeDb();
        
        init(scheduler);
    }

    private void init(ScheduledExecutorService scheduler) {
        scheduler.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                FileWriter fw;
                try {
                    fw = new FileWriter(FILENAME, true);
                } catch (IOException e) {
                    System.out.printf("ERROR: IOException for filename \"%s\"%n", FILENAME);
                    e.printStackTrace();
                    return;
                }
                PrintWriter writer = new PrintWriter(new BufferedWriter(fw));
                
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
                        int totalMinutes = watchTimeDb.getMinutesById(newFollowerId) +
                                           streamTracker.getViewerMinutesById(newFollowerUser.getId());
                        String displayName = newFollowerUser.getDisplayName();
                        writer.write(String.format(
                                "%s New follower: %s - First seen: %s - Watchtime: %d minutes\n",
                                getCurrentDateString(),
                                displayName + " ".repeat(MAX_USERNAME_LENGTH - displayName.length()),
                                prevDateToString(watchTimeDb.getFirstSeenById(newFollowerId)),
                                totalMinutes
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
                        int totalMinutes = watchTimeDb.getMinutesById(unfollowerId) +
                                           streamTracker.getViewerMinutesById(unfollowerUser.getId());
                        String displayName = unfollowerUser.getDisplayName();
                        writer.write(String.format(
                                "%s Unfollower:   %s - First seen: %s - Last seen: %s - Watchtime: %d minutes\n",
                                getCurrentDateString(),
                                displayName + " ".repeat(MAX_USERNAME_LENGTH - displayName.length()),
                                prevDateToString(watchTimeDb.getFirstSeenById(unfollowerId)),
                                prevDateToString(watchTimeDb.getLastSeenById(unfollowerId)),
                                totalMinutes
                        ));
                    } else {
                        String name = watchTimeDb.getNameById(unfollowerId);
                        if (name.isEmpty()) {
                            name = "id: " + unfollowerId;
                        }
                        writer.write(String.format(
                                "%s (deleted):    %s - First seen: %s - Last seen: %s - Watchtime: %d minutes\n",
                                getCurrentDateString(),
                                name + " ".repeat(MAX_USERNAME_LENGTH - name.length()),
                                prevDateToString(watchTimeDb.getFirstSeenById(unfollowerId)),
                                prevDateToString(watchTimeDb.getLastSeenById(unfollowerId)),
                                watchTimeDb.getMinutesById(unfollowerId)
                        ));
                    }
                }
                oldFollowerIdList = updatedFollowerIds;
                writer.close();
            }
        }, 0, INTERVAL, TimeUnit.MINUTES);
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
            return "never     ";
        }
        return dateFormatFollow.format(date);
    }
}
