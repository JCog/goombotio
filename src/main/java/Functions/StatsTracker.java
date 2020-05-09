package Functions;

import Util.Database.WatchTimeDb;
import Util.Settings;
import Util.TwirkInterface;
import com.github.twitch4j.TwitchClient;
import com.github.twitch4j.helix.domain.Follow;
import com.github.twitch4j.helix.domain.FollowList;
import com.github.twitch4j.helix.domain.UserList;

import java.io.File;
import java.util.*;

import static java.lang.System.out;

public class StatsTracker {
    private final static String BLACKLIST_FILENAME = "src/main/resources/blacklist.txt";
    
    private final TwirkInterface twirk;
    private final TwitchClient twitchClient;
    private final StreamInfo streamInfo;
    private final String stream;
    private final String authToken;
    private final int interval;
    private final Timer timer;
    private final HashMap<String, Integer> usersMap;
    private final ArrayList<String> blacklist;
    private final WatchTimeDb watchTimeDb;
    private final Set<String> followers;
    private final String streamId;
    private final ArrayList<Map.Entry<String, Integer>> allTimeViewers;
    
    private HashMap<String, String> userIdMap;
    
    /**Creates an object to track stats about the stream such as viewer watch time and new followers
     * @param twirk Twirk object to communicate with Twitch chat
     * @param twitchClient Twitch API interface
     * @param streamInfo object to collect basic info about the stream
     * @param stream stream name to collect data for
     * @param authToken bot's auth token
     * @param interval how often to collect data in milliseconds
     */
    public StatsTracker(TwirkInterface twirk, TwitchClient twitchClient, StreamInfo streamInfo, int interval) {
        this.twirk = twirk;
        this.twitchClient = twitchClient;
        this.streamInfo = streamInfo;
        this.stream = Settings.getTwitchStream();
        this.authToken = Settings.getTwitchAuthToken();
        this.interval = interval;
        timer = new Timer();
        usersMap = new HashMap<>();
        blacklist = blacklistInit(BLACKLIST_FILENAME);
        watchTimeDb = WatchTimeDb.getInstance();
        streamId = getUserId();
        followers = getFollowers();
        userIdMap = new HashMap<>();
        allTimeViewers = watchTimeDb.getTopUsers();
    }
    
    /**
     * Starts the stat tracking until stop() is run
     */
    public void start() {

        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (streamInfo.isLive()) {
                    for (String user : twirk.getUsersOnline()) {
                        if (!blacklist.contains(user)) {
                            usersMap.putIfAbsent(user, 0);
                            usersMap.put(user, usersMap.get(user) + interval);
                        }
                    }
                }
            }
        }, 0, interval);
    }
    
    /**
     * Stops the stat tracking
     */
    public void stop() {
        timer.cancel();
    }
    
    /**
     * Stores all collected data on watch time for the current session. Be sure to never run more than once or else the
     * data will be inaccurate.
     */
    public void storeAllMinutes() {
        Iterator<Map.Entry<String, Integer>> usersMapIt = usersMap.entrySet().iterator();
        HashMap<String, String> usersIds = getUsersIds();
        while (usersMapIt.hasNext()) {
            Map.Entry<String, Integer> entry = usersMapIt.next();
            String name = entry.getKey();
            String id;
            try {
                id = usersIds.get(name);
            }
            catch (NullPointerException npe) {
                out.println(String.format("Unable to get ID for %s", name));
                npe.printStackTrace();
                continue;
            }
            int minutes = entry.getValue() / (60 * 1000);

            if ( !blacklist.contains(entry.getKey()) ) {
                watchTimeDb.addMinutes(id, name, minutes);
            }
        }
    }
    
    /**
     * Retrieves the viewers that have joined chat for the first time ordered by watch time
     * @return ordered list of <Users, Minutes>
     */
    public ArrayList<Map.Entry<String, Integer>> getNewViewers() {
        ArrayList<Map.Entry<String, Integer>> allTimeViewersCopy = new ArrayList<>(allTimeViewers);
        HashMap<String, Integer> newViewersMap = new HashMap<>(usersMap);
        for (Map.Entry<String, Integer> viewer : allTimeViewersCopy) {
            newViewersMap.remove(viewer.getKey());
        }
        ArrayList<Map.Entry<String, Integer>> newViewersArray = new ArrayList<>(newViewersMap.entrySet());
        newViewersArray.sort(new SortMapDescending());
        return newViewersArray;
    }
    
    /**
     * Retrieves the total minutes a user has been in chat for the current session
     * @param viewer username of the user to get minutes for
     * @return total minutes the user has been in chat
     */
    public int getViewerMinutes(String viewer) {
        return usersMap.get(viewer) / (60 * 1000);
    }
    
    /**
     * Retrieves the viewers that have joined chat during the current session, as well as at least once previously
     * ordered by watch time
     * @return ordered list of <Users, Minutes>
     */
    public ArrayList<Map.Entry<String, Integer>> getReturningViewers() {
        Set<Map.Entry<String, Integer>> currentViewers = usersMap.entrySet();
        HashMap<String, Integer> allTimeViewersCopy = arrayListToHashMap(allTimeViewers);
        ArrayList<Map.Entry<String, Integer>> returningViewers = new ArrayList<>();
        for (Map.Entry<String, Integer> currentViewer : currentViewers) {
            if (allTimeViewersCopy.containsKey(currentViewer.getKey())) {
                returningViewers.add(currentViewer);
            }
        }
        returningViewers.sort(new SortMapDescending());
        return returningViewers;
    }
    
    /**
     * Retrieves a list of all followers for the tracked stream
     * @return Set of usernames
     */
    public Set<String> getFollowers() {
        try {
            FollowList list = twitchClient.getHelix().getFollowers(authToken, null, streamId, null, 100).execute();
            Set<String> followers = new HashSet<>();
            while (list.getFollows().size() > 0) {
                for (Follow follower : list.getFollows()) {
                    followers.add(follower.getFromName());
                }
                list = twitchClient.getHelix().getFollowers(authToken, null, streamId, list.getPagination().getCursor(), 100).execute();
            }
            return followers;
        }
        catch (Exception e) {
            e.printStackTrace();
            return new HashSet<>();
        }
    }
    
    /**
     * Retrieves a list of followers the tracked stream had at the beginning of the session
     * @return Set of usernames
     */
    public Set<String> getStartingFollowers() {
        return new HashSet<>(followers);
    }
    
    /**
     * Retrieves a copy of usersMap, which contains the watch time of all viewers during the current session
     * @return HashMap of <Users, Milliseconds>
     */
    public HashMap<String, Integer> getUsersMapCopy() {
        return new HashMap<>(usersMap);
    }
    
    /**
     * Retrieves a list of all viewers that joined chat during the current session and orders them by how many followers
     * they have
     * @return ArrayList of <username, followers>
     */
    public ArrayList<Map.Entry<String, Integer>> getTopFollowerCounts() {
        ArrayList<Map.Entry<String, Integer>> followerCounts = new ArrayList<>();
        Set<Map.Entry<String, String>> userIds = getUsersIds().entrySet();

        for(Map.Entry<String, String> entry : userIds) {
            FollowList userFollows = twitchClient.getHelix().getFollowers(authToken, null, entry.getValue(), null, 1).execute();
            String name = entry.getKey();
            int followCount = userFollows.getTotal();
            followerCounts.add(new AbstractMap.SimpleEntry<>(name, followCount));
        }
        followerCounts.sort(new SortMapDescending());
        return followerCounts;
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

    private HashMap<String, String> getUsersIds() {
        if (userIdMap.size() != usersMap.size()) {
            HashMap<String, String> userIds = new HashMap<>();
            Iterator<Map.Entry<String, Integer>> usersMapIt = usersMap.entrySet().iterator();
            List<String> usersHundred = new ArrayList<>();
            while (usersMapIt.hasNext()) {
                while (usersHundred.size() < 100 && usersMapIt.hasNext()) {
                    usersHundred.add(usersMapIt.next().getKey());
                }
                UserList resultList = twitchClient.getHelix().getUsers(authToken, null, usersHundred).execute();
                resultList.getUsers().forEach(user -> userIds.put(user.getLogin(), user.getId()));
                usersHundred.clear();
            }
            userIdMap = userIds;
            return userIds;
        }
        else {
            return userIdMap;
        }
    }

    private String getUserId() {
        UserList resultList = twitchClient.getHelix().getUsers(authToken, null, Collections.singletonList(stream)).execute();
        return resultList.getUsers().get(0).getId();
    }

    private static class SortMapDescending implements Comparator<Map.Entry<String, Integer>> {

        @Override
        public int compare(Map.Entry<String, Integer> o1, Map.Entry<String, Integer> o2) {
            return o2.getValue() - o1.getValue();
        }
    }

    private static HashMap<String, Integer> arrayListToHashMap(ArrayList<Map.Entry<String, Integer>> arrayList) {
        HashMap<String, Integer> hashMap = new HashMap<>();
        for (Map.Entry<String, Integer> entry : arrayList) {
            hashMap.put(entry.getKey(), entry.getValue());
        }
        return hashMap;
    }
}
