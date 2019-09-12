package Functions;

import Util.Database.WatchTimeDb;
import Util.FileWriter;
import com.gikk.twirk.Twirk;
import com.github.twitch4j.TwitchClient;
import com.github.twitch4j.helix.domain.Follow;
import com.github.twitch4j.helix.domain.FollowList;
import com.github.twitch4j.helix.domain.UserList;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static java.lang.System.out;

public class StatsTracker {
    private final static String BLACKLIST_FILENAME = "src/main/resources/blacklist.txt";
    private final static String REPORT_LOCATION = "streamreports/";

    private Twirk twirk;
    private TwitchClient twitchClient;
    private StreamInfo streamInfo;
    private String stream;
    private String authToken;
    private int interval;
    private Timer timer;
    private HashMap<String, Integer> usersMap;
    private ArrayList<String> blacklist;
    private WatchTimeDb watchTimeDb;
    private Set<String> followers;
    private long streamId;
    private HashMap<String, Long> userIdMap;
    private ArrayList<Map.Entry<String, Integer>> allTimeViewers;

    public StatsTracker(Twirk twirk, TwitchClient twitchClient, StreamInfo streamInfo, String stream, String authToken, int interval) {
        this.twirk = twirk;
        this.twitchClient = twitchClient;
        this.streamInfo = streamInfo;
        this.stream = stream;
        this.authToken = authToken;
        this.interval = interval;
        timer = new Timer();
        usersMap = new HashMap<>();
        blacklist = blacklistInit(BLACKLIST_FILENAME);
        watchTimeDb = new WatchTimeDb();
        streamId = getUserId();
        followers = getFollowers();
        userIdMap = new HashMap<>();
        allTimeViewers = watchTimeDb.getTopUsers();
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

    private ArrayList<Map.Entry<String, Integer>> getNewViewers() {
        Set<Map.Entry<String, Integer>> currentViewers = usersMap.entrySet();
        ArrayList<Map.Entry<String, Integer>> allTimeViewersCopy = new ArrayList<>(allTimeViewers);
        HashMap<String, Integer> newViewersMap = new HashMap<>(usersMap);
        for (Map.Entry<String, Integer> viewer : allTimeViewersCopy) {
            newViewersMap.remove(viewer.getKey());
        }
        ArrayList<Map.Entry<String, Integer>> newViewersArray = new ArrayList<>(newViewersMap.entrySet());
        newViewersArray.sort(new SortMapDescending());
        return newViewersArray;
    }

    private int getViewerMinutes(String viewer) {
        return usersMap.get(viewer) / (60 * 1000);
    }

    private ArrayList<Map.Entry<String, Integer>> getReturningViewers() {
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

    public void generateReport() {
        StringBuilder report = new StringBuilder();
        StringBuilder streamStatsReport = new StringBuilder();
        StringBuilder allViewersReport = new StringBuilder();
        StringBuilder newViewersReport = new StringBuilder();
        StringBuilder returningViewersReport = new StringBuilder();

        //      Stream Stats Report
        streamStatsReport.append("------ Stream Stats ------\n");
        int averageViewers = streamInfo.getAverageViewers();
        int medianViewers = streamInfo.getMedianViewers();
        int maxViewers = streamInfo.getMaxViewers();
        streamStatsReport.append(String.format("Average Viewer Count: %d\n", averageViewers));
        streamStatsReport.append(String.format("Median Viewer Count:  %d\n", medianViewers));
        streamStatsReport.append(String.format("Max Viewer Count:     %d\n", maxViewers));

        //      All Viewers Report
        int allWatchTime = 0;
        allViewersReport.append("------ All Viewers ------\n");
        ArrayList<Map.Entry<String, Integer>> biggestViewers = getTopFollowerCounts();
        allViewersReport.append("Biggest Viewers:\n");
        for (int i = 0; i < 10 && i < biggestViewers.size(); i++) {
            String name = biggestViewers.get(i).getKey();
            int followers = biggestViewers.get(i).getValue();
            int minutes = getViewerMinutes(name);
            allViewersReport.append(String.format("%d. %s: %d followers, %d minutes\n", i + 1, name, followers, minutes));
        }
        allViewersReport.append("\n");
        for (Integer value : usersMap.values()) {
            allWatchTime += value / (60 * 1000);
        }
        int averageAllMinutes = allWatchTime / usersMap.size();
        allViewersReport.append(String.format("Total Viewers: %d viewers\n", usersMap.size()));
        allViewersReport.append(String.format("Average Watchtime: %d minutes\n", averageAllMinutes));

        //      New Viewers Report
        ArrayList<Map.Entry<String, Integer>> newViewersSet = getNewViewers();
        int newWatchTime = 0;
        newViewersReport.append("------ New Viewers ------\n");
        for (Map.Entry<String, Integer> viewer : newViewersSet) {
            int minutes = viewer.getValue() / (60 * 1000);
            newWatchTime += minutes;
            newViewersReport.append(String.format("%s: %d\n", viewer.getKey(), minutes));
        }
        newViewersReport.append("\n");
        //New Followers
        Set<String> newFollowers = getFollowers();
        newFollowers.removeAll(followers);
        newViewersReport.append("New Followers:\n");
        for (String follower : newFollowers) {
            newViewersReport.append(String.format("%s\n", follower));
        }
        newViewersReport.append("\n");

        int averageNewMinutes = newWatchTime / newViewersSet.size();
        //int percentWhoFollowed = newFollowers.size() / newViewersSet.size();
        newViewersReport.append(String.format("Total New Viewers: %d viewers\n", newViewersSet.size()));
        newViewersReport.append(String.format("Average Watchtime: %d minutes\n", averageNewMinutes));
        //newViewersReport.append(String.format("Percent of New Viewers who followed: %d%%", percentWhoFollowed));

        //      Returning Viewers Report
        ArrayList<Map.Entry<String, Integer>> returningViewersList = getReturningViewers();
        int returningWatchTime = 0;
        returningViewersReport.append("------ Returning Viewers ------\n");
        for (Map.Entry<String, Integer> viewer : returningViewersList) {
            int minutes = viewer.getValue() / (60 * 1000);
            returningWatchTime += minutes;
            returningViewersReport.append(String.format("%s: %d\n", viewer.getKey(), minutes));
        }
        returningViewersReport.append("\n");

        int averageReturningMinutes = returningWatchTime / returningViewersList.size();
        returningViewersReport.append(String.format("Total Returning Viewers: %d viewers\n", returningViewersList.size()));
        returningViewersReport.append(String.format("Average Watchtime: %d minutes\n", averageReturningMinutes));

        //      Put it all together
        report.append("REPORT\n\n");
        report.append(streamStatsReport);
        report.append("\n\n");
        report.append(allViewersReport);
        report.append("\n\n");
        report.append(newViewersReport);
        report.append("\n\n");
        report.append(returningViewersReport);

        LocalDateTime date = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ENGLISH);
        String filename = "StreamReport" + formatter.format(date) + ".txt";

        FileWriter.writeToFile(REPORT_LOCATION, filename, report.toString());
    }

    private Set<String> getFollowers() {
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

    private ArrayList<Map.Entry<String, Integer>> getTopFollowerCounts() {
        ArrayList<Map.Entry<String, Integer>> followerCounts = new ArrayList<>();
        Set<Map.Entry<String, Long>> userIds = getUsersIds().entrySet();

        for(Map.Entry<String, Long> entry : userIds) {
            FollowList userFollows = twitchClient.getHelix().getFollowers(authToken, null, entry.getValue(), null, 1).execute();
            String name = entry.getKey();
            int followCount = userFollows.getTotal();
            followerCounts.add(new AbstractMap.SimpleEntry<>(name, followCount));
        }
        Collections.sort(followerCounts, new SortMapDescending());
        return followerCounts;
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
