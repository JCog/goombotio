package functions;

import com.github.twitch4j.helix.domain.User;
import com.jcog.utils.TwitchApi;
import com.jcog.utils.database.DbManager;
import com.jcog.utils.database.stats.StreamStatsDb;
import com.jcog.utils.database.stats.WatchTimeDb;
import com.netflix.hystrix.exception.HystrixRuntimeException;

import java.util.*;
import java.util.concurrent.TimeUnit;

import static java.lang.System.out;

public class StreamData {

    private final HashMap<String,Integer> userMinutes = new HashMap<>();
    private final ArrayList<Integer> viewerCounts = new ArrayList<>();
    private final ArrayList<User> newViewers = new ArrayList<>();
    private final ArrayList<User> returningViewers = new ArrayList<>();

    private final StreamStatsDb streamStatsDb;
    private final WatchTimeDb watchTimeDb;
    private final TwitchApi twitchApi;
    private final User streamerUser;
    private final Date startTime;

    private Date endTime;

    public StreamData(DbManager dbManager, TwitchApi twitchApi, User streamerUser) {
        this.twitchApi = twitchApi;
        this.streamerUser = streamerUser;
        streamStatsDb = dbManager.getStreamStatsDb();
        watchTimeDb = dbManager.getWatchTimeDb();

        out.println("---------------------");
        out.println(streamerUser.getDisplayName() + " is now live.");
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
        out.println(streamerUser.getDisplayName() + " has gone offline.");
        out.println("---------------------");
        endTime = new Date();

        List<User> userList;
        try {
            userList = twitchApi.getUserListByUsernames(userMinutes.keySet());
        }
        catch (HystrixRuntimeException e) {
            e.printStackTrace();
            out.println("Error retrieving user data for stream, unable to save stream statistics");
            return;
        }
        //make sure this function is run before updating the database
        separateNewReturningViewers(userList);
        streamStatsDb.addStream(watchTimeDb, startTime, endTime, viewerCounts, userMinutes);

        for (User user : userList) {
            int minutes = userMinutes.get(user.getLogin());
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
        ArrayList<Integer> viewersCounts = new ArrayList<>(viewerCounts);
        Collections.sort(viewersCounts);
        boolean isEven = viewersCounts.size() % 2 == 0;
        int middleIndex = viewersCounts.size() / 2;

        if (viewersCounts.size() == 0) {
            return 0;
        }
        else if (isEven) {
            int first = viewersCounts.get(middleIndex - 1);
            int second = viewersCounts.get(middleIndex);
            return (first + second) / 2;
        }
        else {
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

    public ArrayList<User> getNewViewers() {
        return newViewers;
    }

    public ArrayList<User> getReturningViewers() {
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

    public int getViewerMinutes(String username) {
        Integer minutes = userMinutes.get(username.toLowerCase());
        return Objects.requireNonNullElse(minutes, 0);
    }

    public HashMap<String,Integer> getAllViewerMinutes() {
        return userMinutes;
    }

    //probably want to replace this with something better at some point
    public ArrayList<Map.Entry<String,Integer>> getOrderedWatchtimeList(List<User> userList) {
        ArrayList<Map.Entry<String,Integer>> output = new ArrayList<>();
        for (User user : userList) {
            output.add(new AbstractMap.SimpleEntry<>(
                    user.getDisplayName(),
                    userMinutes.get(user.getLogin())
            ));
        }
        output.sort(new SortMapDescending());
        return output;
    }

    //probably want to replace this with something better at some point
    public ArrayList<Map.Entry<String,Integer>> getTopFollowerCounts() {
        ArrayList<Map.Entry<String,Integer>> followerCounts = new ArrayList<>();
        ArrayList<User> allViewers = new ArrayList<>(newViewers);
        allViewers.addAll(returningViewers);

        for (User user : allViewers) {
            int followCount;
            try {
                followCount = twitchApi.getFollowerCount(user.getId());
            }
            catch (HystrixRuntimeException e) {
                e.printStackTrace();
                out.printf("Error retrieving follower count for %s%n", user.getDisplayName());
                continue;
            }
            followerCounts.add(new AbstractMap.SimpleEntry<>(user.getDisplayName(), followCount));
        }
        followerCounts.sort(new SortMapDescending());
        return followerCounts;
    }

    ///////////////////////////////////////////////////////////////////////////

    private void separateNewReturningViewers(List<User> userList) {
        HashSet<Long> allTimeUserIds = watchTimeDb.getAllUserIds();
        returningViewers.clear();
        newViewers.clear();
        for (User user : userList) {
            long userId = Long.parseLong(user.getId());
            if (allTimeUserIds.contains(userId)) {
                returningViewers.add(user);
            }
            else {
                newViewers.add(user);
            }
        }
    }

    private static class SortMapDescending implements Comparator<Map.Entry<String,Integer>> {

        @Override
        public int compare(Map.Entry<String,Integer> o1, Map.Entry<String,Integer> o2) {
            return o2.getValue() - o1.getValue();
        }
    }
}
