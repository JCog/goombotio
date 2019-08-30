package Functions;

import Util.Database.WatchTimeDb;
import com.gikk.twirk.Twirk;
import com.github.twitch4j.TwitchClient;
import com.github.twitch4j.helix.domain.UserList;

import java.io.File;
import java.util.*;

import static java.lang.System.out;

public class ViewerTracker {
    private final static String BLACKLIST_FILENAME = "src/main/resources/blacklist.txt";

    private Twirk twirk;
    private TwitchClient twitchClient;
    private int interval;
    private Timer timer;
    private HashMap<String, Integer> usersMap;
    private ArrayList<String> blacklist;
    private WatchTimeDb watchTimeDb;

    public ViewerTracker(Twirk twirk, TwitchClient twitchClient, int interval) {
        this.twirk = twirk;
        this.twitchClient = twitchClient;
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
                for (String user : twirk.getUsersOnline()) {
                    usersMap.putIfAbsent(user, 0);
                    usersMap.put(user, usersMap.get(user) + interval);
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

    public void printViewersByViewTime() {
        Set<Map.Entry<String, Integer>> usersSet = usersMap.entrySet();
        ArrayList<Map.Entry<String, Integer>> orderedUsers = new ArrayList<>(usersSet);
        orderedUsers.sort(new SortByViewTime());

        out.println("Top Viewers (Minutes)\n---------------------\n");
        for (Map.Entry<String, Integer> entry : orderedUsers) {
            if ( !blacklist.contains(entry.getKey()) ) {
                int minutes = entry.getValue() / (60 * 1000);
                out.println(entry.getKey() + ": " + minutes);
            }
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

    private class SortByViewTime implements Comparator<Map.Entry<String, Integer>> {

        @Override
        public int compare(Map.Entry<String, Integer> o1, Map.Entry<String, Integer> o2) {
            return o2.getValue() - o1.getValue();
        }
    }
}
