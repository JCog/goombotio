package functions;

import com.github.twitch4j.helix.domain.User;
import com.netflix.hystrix.exception.HystrixRuntimeException;
import database.stats.StreamStatsDb;
import database.stats.WatchTimeDb;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.CommonUtils;
import util.TwitchApi;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class StreamData {
    private static final Logger log = LoggerFactory.getLogger(StreamData.class);

    private final Map<String,Integer> userIdMinutesMap = new HashMap<>();
    private final List<Integer> viewerCounts = new ArrayList<>();
    private final List<User> newViewers = new ArrayList<>();
    private final List<User> returningViewers = new ArrayList<>();

    private final StreamStatsDb streamStatsDb;
    private final WatchTimeDb watchTimeDb;
    private final TwitchApi twitchApi;
    private final Date startTime;

    private Date endTime;

    public StreamData(CommonUtils commonUtils) {
        twitchApi = commonUtils.twitchApi();
        streamStatsDb = commonUtils.dbManager().getStreamStatsDb();
        watchTimeDb = commonUtils.dbManager().getWatchTimeDb();

        log.info("{} is now live.", twitchApi.getStreamerUser().getDisplayName());
        startTime = new Date();
    }

    public void updateUsersMinutes(Collection<String> userIdList) {
        for (String user : userIdList) {
            userIdMinutesMap.putIfAbsent(user, 0);
            userIdMinutesMap.put(user, userIdMinutesMap.get(user) + 1);
        }
    }

    public void updateStreamViewCount(int viewCount) {
        viewerCounts.add(viewCount);
    }

    public void endStream() {
        log.info("{} has gone offline.", twitchApi.getStreamerUser().getDisplayName());
        endTime = new Date();

        List<User> userList;
        try {
            userList = twitchApi.getUserListByIds(userIdMinutesMap.keySet());
        } catch (HystrixRuntimeException e) {
            log.error("Error retrieving user data for stream, unable to save stream statistics: {}", e.getMessage());
            return;
        }
        //make sure this function is run before updating the database
        separateNewReturningViewers(userList);
        streamStatsDb.addStream(watchTimeDb, startTime, endTime, viewerCounts, userIdMinutesMap, userList);

        for (User user : userList) {
            int minutes = userIdMinutesMap.get(user.getId());
            watchTimeDb.addMinutes(user.getId(), user.getLogin(), minutes);
        }
    }

    ///////////////////////////////////////////////////////////////////////////

    public int getAverageViewers() {
        int sum = 0;
        for (Integer count : viewerCounts) {
            sum += count;
        }
        if (viewerCounts.size() == 0) {
            return 0;
        }
        return sum / viewerCounts.size();
    }

    public int getMedianViewers() {
        List<Integer> viewersCounts = new ArrayList<>(viewerCounts);
        Collections.sort(viewersCounts);
        boolean isEven = viewersCounts.size() % 2 == 0;
        int middleIndex = viewersCounts.size() / 2;

        if (viewersCounts.size() == 0) {
            return 0;
        } else if (isEven) {
            int first = viewersCounts.get(middleIndex - 1);
            int second = viewersCounts.get(middleIndex);
            return (first + second) / 2;
        } else {
            return viewersCounts.get(middleIndex);
        }
    }

    public int getMaxViewers() {
        int max = 0;
        for (Integer count : viewerCounts) {
            max = Math.max(max, count);
        }
        return max;
    }

    public List<User> getNewViewers() {
        return newViewers;
    }

    public List<User> getReturningViewers() {
        return returningViewers;
    }

    //stream length in minutes
    public int getStreamLength() {
        if (startTime == null) {
            return 0;
        }

        Date endTemp = (endTime == null ? new Date() : endTime);
        long duration = endTemp.getTime() - startTime.getTime();
        return Math.toIntExact(TimeUnit.MILLISECONDS.toMinutes(duration));
    }

    public int getViewerMinutesById(String userId) {
        Integer minutes = userIdMinutesMap.get(userId);
        return Objects.requireNonNullElse(minutes, 0);
    }

    public Map<String,Integer> getAllViewerMinutesById() {
        return userIdMinutesMap;
    }

    //probably want to replace this with something better at some point
    public List<Map.Entry<User,Integer>> getOrderedWatchtimeList(List<User> userList) {
        List<Map.Entry<User,Integer>> output = new ArrayList<>();
        for (User user : userList) {
            output.add(new AbstractMap.SimpleEntry<>(
                    user,
                    userIdMinutesMap.get(user.getId())
            ));
        }
        output.sort(new SortUserMapDescending());
        return output;
    }

    //probably want to replace this with something better at some point
    public List<Map.Entry<User,Integer>> getTopFollowerCounts() {
        List<Map.Entry<User,Integer>> followerCounts = new ArrayList<>();
        List<User> allViewers = new ArrayList<>(newViewers);
        allViewers.addAll(returningViewers);

        for (User user : allViewers) {
            int followCount;
            try {
                followCount = twitchApi.getChannelFollowersCount(user.getId());
            } catch (HystrixRuntimeException e) {
                log.error("Error retrieving follower count for {}: {}", user.getDisplayName(), e.getMessage());
                continue;
            }
            followerCounts.add(new AbstractMap.SimpleEntry<>(user, followCount));
        }
        followerCounts.sort(new SortUserMapDescending());
        return followerCounts;
    }

    ///////////////////////////////////////////////////////////////////////////

    private void separateNewReturningViewers(List<User> userList) {
        Set<String> allTimeUserIds = watchTimeDb.getAllUserIds();
        returningViewers.clear();
        newViewers.clear();
        for (User user : userList) {
            if (allTimeUserIds.contains(user.getId())) {
                returningViewers.add(user);
            } else {
                newViewers.add(user);
            }
        }
    }
    
    private static class SortUserMapDescending implements Comparator<Map.Entry<User,Integer>> {
        
        @Override
        public int compare(Map.Entry<User,Integer> o1, Map.Entry<User,Integer> o2) {
            return o2.getValue() - o1.getValue();
        }
    }
}
