package Util;

import Database.Stats.StreamStatsDb;
import Database.Stats.WatchTimeDb;
import com.github.twitch4j.TwitchClient;
import com.github.twitch4j.helix.domain.FollowList;
import com.github.twitch4j.helix.domain.UserList;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class StreamStatsInterface {
    private final StreamStatsDb streamStatsDb;
    private final WatchTimeDb watchTimeDb;
    private final TwitchClient twitchClient;
    private final String authToken;
    
    /**
     * Interface for saving and reading information about streams in a database
     */
    public StreamStatsInterface(TwitchClient twitchClient, String authToken){
        streamStatsDb = StreamStatsDb.getInstance();
        watchTimeDb = WatchTimeDb.getInstance();
        this.twitchClient = twitchClient;
        this.authToken = authToken;
    }
    
    /**
     * Returns the average concurrent viewers watching the stream during the most recent stream.
     * @return average viewer count
     */
    public int getAverageViewers() {
        List<Integer> viewerCounts = streamStatsDb.getViewerCounts();
        int sum = 0;
        for (Integer count : viewerCounts) {
            sum += count;
        }
        if (viewerCounts.size() == 0) {
            return 0;
        }
        return sum / viewerCounts.size();
    }
    
    /**
     * Returns the median concurrent viewers watching the stream during the most recent stream.
     * @return median viewer count
     */
    public int getMedianViewers() {
        List<Integer> viewerCounts = streamStatsDb.getViewerCounts();
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
    
    /**
     * Returns the maximum concurrent viewers watching the stream during the most recent stream.
     * @return average viewer count
     */
    public int getMaxViewers() {
        List<Integer> viewerCounts = streamStatsDb.getViewerCounts();
        int max = 0;
        for (Integer count : viewerCounts) {
            max = Math.max(max, count);
        }
        return max;
    }
    
    /**
     * Returns the list of users who watched the most recent stream
     * @return user list
     */
    public List<String> getUserList() {
        return streamStatsDb.getUserList();
    }
    
    /**
     * Returns the list of users who watched the most recent stream for the first time
     * @return new user list
     */
    public List<String> getNewUserList() {
        return streamStatsDb.getNewUserList();
    }
    
    /**
     * Returns the list of users who watched the most recent stream and have also watched at least one prior stream
     * @return returning user list
     */
    public List<String> getReturningUserList() {
        return streamStatsDb.getReturningUserList();
    }
    
    /**
     * Returns a map of the watchtime for each user from the most recent stream
     * @return user watch time map
     */
    public HashMap<String, Integer> getUserMinutesList() {
        return streamStatsDb.getUserMinutesList();
    }
    
    /**
     * Returns the time the most recent stream started
     * @return start time
     */
    public Date getStartTime() {
        return streamStatsDb.getStreamStartTime();
    }
    
    /**
     * Returns the time the most recent stream ended
     * @return end time
     */
    public Date getEndTime() {
        return streamStatsDb.getStreamEndTime();
    }
    
    /**
     * Returns the time since the start of the stream in minutes
     * @return stream length in minutes
     */
    public int getStreamLength() {
        Date startTime = streamStatsDb.getStreamStartTime();
        Date endTime = streamStatsDb.getStreamEndTime();
        if (startTime == null || endTime == null) {
            return 0;
        }
        long duration  = endTime.getTime() - startTime.getTime();
        return Math.toIntExact(TimeUnit.MILLISECONDS.toMinutes(duration));
    }
    
    /**
     * Retrieves a list of all viewers that joined chat during the most recent stream and orders them by how many followers
     * they have. Contains an expensive Twitch API call.
     * @return ArrayList of <username, followers>
     */
    public ArrayList<Map.Entry<String, Integer>> getTopFollowerCounts() {
        ArrayList<Map.Entry<String, Integer>> followerCounts = new ArrayList<>();
        Set<Map.Entry<String, String>> userIds = getUsersIds(streamStatsDb.getUserList()).entrySet();
        
        for(Map.Entry<String, String> entry : userIds) {
            FollowList userFollows = twitchClient.getHelix().getFollowers(authToken, null, entry.getValue(), null, 1).execute();
            String name = entry.getKey();
            int followCount = userFollows.getTotal();
            followerCounts.add(new AbstractMap.SimpleEntry<>(name, followCount));
        }
        followerCounts.sort(new SortMapDescending());
        return followerCounts;
    }
    
    /**
     * Returns the average time since all users have first seen the stream as of the most recent stream in days
     * @return average viewer age in days
     */
    public int getAverageViewerAge() {
        int totalAge = 0;
        Date streamDate = simplifyDate(streamStatsDb.getStreamEndTime());
        List<String> usersMap = streamStatsDb.getUserList();
        for(String name : usersMap) {
            Date firstSeen = watchTimeDb.getFirstSeen(name);
            int ageDays = Math.toIntExact(TimeUnit.DAYS.convert(streamDate.getTime() - firstSeen.getTime(), TimeUnit.MILLISECONDS));
        
            totalAge += ageDays;
        }
        return totalAge / usersMap.size();
    }
    
    /**
     * Returns the average time since all users have first seen the stream as of the most recent stream, weighted by
     * time spent watching the most recent stream, in days
     * @return weighted average viewer age in days
     */
    public int getWeightedViewerAge() {
        int weightedAgeNumer = 0;
        int weightedAgeDenom = 0;
        Date streamDate = simplifyDate(streamStatsDb.getStreamEndTime());
        Set<Map.Entry<String, Integer>> userMinutes = streamStatsDb.getUserMinutesList().entrySet();
        for (Map.Entry<String, Integer> entry : userMinutes) {
            String name = entry.getKey();
            int minutes = entry.getValue() / 1000;
            Date firstSeen = watchTimeDb.getFirstSeen(name);
            int ageDays = Math.toIntExact(TimeUnit.DAYS.convert(streamDate.getTime() - firstSeen.getTime(), TimeUnit.MILLISECONDS));
            
            weightedAgeNumer += ageDays * minutes;
            weightedAgeDenom += minutes;
        }
        return weightedAgeNumer / weightedAgeDenom;
    }
    
    //TODO: replace all uses of this
    private HashMap<String, String> getUsersIds(List<String> usersList) {
        HashMap<String, String> userIds = new HashMap<>();
        Iterator<String> usersMapIt = usersList.iterator();
        List<String> usersHundred = new ArrayList<>();
        while (usersMapIt.hasNext()) {
            while (usersHundred.size() < 100 && usersMapIt.hasNext()) {
                usersHundred.add(usersMapIt.next());
            }
            UserList resultList = twitchClient.getHelix().getUsers(authToken, null, usersHundred).execute();
            resultList.getUsers().forEach(user -> userIds.put(user.getLogin(), user.getId()));
            usersHundred.clear();
        }
        return userIds;
    }
    
    private static class SortMapDescending implements Comparator<Map.Entry<String, Integer>> {
        
        @Override
        public int compare(Map.Entry<String, Integer> o1, Map.Entry<String, Integer> o2) {
            return o2.getValue() - o1.getValue();
        }
    }
    
    private static Date simplifyDate(Date date) {
        Calendar calendar = new GregorianCalendar();
        calendar.setTime(date);
        calendar.set(Calendar.HOUR_OF_DAY, 12);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }
}
