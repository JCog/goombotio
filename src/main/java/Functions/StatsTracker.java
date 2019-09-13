package Functions;

import Util.Database.WatchTimeDb;
import com.gikk.twirk.Twirk;
import com.github.twitch4j.TwitchClient;
import com.github.twitch4j.helix.domain.Follow;
import com.github.twitch4j.helix.domain.FollowList;
import com.github.twitch4j.helix.domain.UserList;

import java.io.File;
import java.util.*;

import static java.lang.System.out;

public class StatsTracker {
    private final static String BLACKLIST_FILENAME = "src/main/resources/blacklist.txt";

    private Twirk twirk;
    private TwitchClient twitchClient;
    private StreamInfo streamInfo;
    private String stream;
    private String authToken;
    private int interval;
    private Timer timer = new Timer();
    private HashMap<String, Integer> usersMap = new HashMap<>();
    private ArrayList<String> blacklist = blacklistInit(BLACKLIST_FILENAME);
    private WatchTimeDb watchTimeDb = new WatchTimeDb();;
    private Set<String> followers = getFollowers();
    private long streamId = getUserId();
    private HashMap<String, Long> userIdMap = new HashMap<>();;
    private ArrayList<Map.Entry<String, Integer>> allTimeViewers = watchTimeDb.getTopUsers();;

    public StatsTracker(Twirk twirk, TwitchClient twitchClient, StreamInfo streamInfo, String stream, String authToken, int interval) {
        this.twirk = twirk;
        this.twitchClient = twitchClient;
        this.streamInfo = streamInfo;
        this.stream = stream;
        this.authToken = authToken;
        this.interval = interval;
    }

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

    public void stop() {
        timer.cancel();
    }

    public void storeAllMinutes() {
        Iterator<Map.Entry<String, Integer>> usersMapIt = usersMap.entrySet().iterator();
        HashMap<String, Long> usersIds = getUsersIds();
        while (usersMapIt.hasNext()) {
            Map.Entry<String, Integer> entry = usersMapIt.next();
            String name = entry.getKey();
            long id;
            try {
                id = usersIds.get(name);
            }
            catch (NullPointerException npe) {
                npe.printStackTrace();
                continue;
            }
            int minutes = entry.getValue() / (60 * 1000);

            if ( !blacklist.contains(entry.getKey()) ) {
                watchTimeDb.addMinutes(id, name, minutes);
            }
        }
    }

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

    public int getViewerMinutes(String viewer) {
        return usersMap.get(viewer) / (60 * 1000);
    }

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
    
    public Set<String> getStartingFollowers() {
        return new HashSet<>(followers);
    }
    
    public HashMap<String, Integer> getUsersMapCopy() {
        return new HashMap<>(usersMap);
    }

    public ArrayList<Map.Entry<String, Integer>> getTopFollowerCounts() {
        ArrayList<Map.Entry<String, Integer>> followerCounts = new ArrayList<>();
        Set<Map.Entry<String, Long>> userIds = getUsersIds().entrySet();

        for(Map.Entry<String, Long> entry : userIds) {
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
        }
        catch (Exception e) {
            out.println(Arrays.toString(e.getStackTrace()));
        }
        
        return blacklist;
    }

    private HashMap<String, Long> getUsersIds() {
        if (userIdMap.size() != usersMap.size()) {
            HashMap<String, Long> userIds = new HashMap<>();
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

    private long getUserId() {
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
