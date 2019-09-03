package Functions;

import Util.Database.WatchTimeDb;
import Util.FileWriter;
import com.gikk.twirk.Twirk;
import com.github.twitch4j.TwitchClient;
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
    private int interval;
    private Timer timer;
    private HashMap<String, Integer> usersMap;
    private ArrayList<String> blacklist;
    private WatchTimeDb watchTimeDb;

    public StatsTracker(Twirk twirk, TwitchClient twitchClient, StreamInfo streamInfo, int interval) {
        this.twirk = twirk;
        this.twitchClient = twitchClient;
        this.streamInfo = streamInfo;
        this.interval = interval;
        timer = new Timer();
        usersMap = new HashMap<>();
        blacklist = blacklistInit(BLACKLIST_FILENAME);
        watchTimeDb = new WatchTimeDb();
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
            long id = usersIds.get(name);
            int minutes = entry.getValue() / (60 * 1000);

            if ( !blacklist.contains(entry.getKey()) ) {
                watchTimeDb.addMinutes(id, name, minutes);
            }
        }
    }

    private ArrayList<Map.Entry<String, Integer>> getNewViewers() {
        Set<Map.Entry<String, Integer>> currentViewers = usersMap.entrySet();
        ArrayList<Map.Entry<String, Integer>> allTimeViewers = new ArrayList<>(watchTimeDb.getTopUsers());
        HashMap<String, Integer> newViewersMap = new HashMap<>(usersMap);
        for (Map.Entry<String, Integer> viewer : allTimeViewers) {
            newViewersMap.remove(viewer.getKey());
        }
        ArrayList<Map.Entry<String, Integer>> newViewersArray = new ArrayList<>(newViewersMap.entrySet());
        newViewersArray.sort(new SortByViewTime());
        return newViewersArray;
    }

    private ArrayList<Map.Entry<String, Integer>> getReturningViewers() {
        Set<Map.Entry<String, Integer>> currentViewers = usersMap.entrySet();
        HashMap<String, Integer> allTimeViewers = arrayListToHashMap(watchTimeDb.getTopUsers());
        ArrayList<Map.Entry<String, Integer>> returningViewers = new ArrayList<>();
        for (Map.Entry<String, Integer> currentViewer : currentViewers) {
            if (allTimeViewers.containsKey(currentViewer.getKey())) {
                returningViewers.add(currentViewer);
            }
        }
        returningViewers.sort(new SortByViewTime());
        return returningViewers;
    }

    public void generateReport() {
        StringBuilder report = new StringBuilder();
        StringBuilder allViewersReport = new StringBuilder();
        StringBuilder newViewersReport = new StringBuilder();
        StringBuilder returningViewersReport = new StringBuilder();

        //      All Viewers Report
        int allWatchTime = 0;
        allViewersReport.append("------ All Viewers ------\n");
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

        int averageNewMinutes = newWatchTime / newViewersSet.size();
        newViewersReport.append(String.format("Total New Viewers: %d viewers\n", newViewersSet.size()));
        newViewersReport.append(String.format("Average Watchtime: %d minutes\n", averageNewMinutes));

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
        HashMap<String, Long> userIds = new HashMap<>();
        Iterator<Map.Entry<String, Integer>> usersMapIt = usersMap.entrySet().iterator();
        List<String> usersHundred = new ArrayList<>();
        while (usersMapIt.hasNext()) {
            while (usersHundred.size() < 100 && usersMapIt.hasNext()) {
                usersHundred.add(usersMapIt.next().getKey());
            }
            UserList resultList = twitchClient.getHelix().getUsers(null, null, usersHundred).execute();
            resultList.getUsers().forEach(user -> userIds.put(user.getLogin(), user.getId()));
            usersHundred.clear();
        }
        return userIds;
    }

    private static class SortByViewTime implements Comparator<Map.Entry<String, Integer>> {

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
