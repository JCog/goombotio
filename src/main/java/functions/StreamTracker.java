package functions;

import com.github.twitch4j.helix.domain.Stream;
import com.github.twitch4j.tmi.domain.Chatters;
import com.netflix.hystrix.exception.HystrixRuntimeException;
import database.DbManager;
import util.ReportBuilder;
import util.TwitchApi;

import java.io.File;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;
import java.util.TimerTask;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class StreamTracker {
    private static final String BLACKLIST_FILENAME = "blacklist.txt";
    private static final int INTERVAL = 1; //minutes

    private final Set<String> blacklist = blacklistInit();

    private final DbManager dbManager;
    private final TwitchApi twitchApi;
    private final ScheduledExecutorService scheduler;

    private StreamData streamData;
    private ScheduledFuture<?> scheduledFuture;

    public StreamTracker(DbManager dbManager, TwitchApi twitchApi, ScheduledExecutorService scheduler) {
        this.dbManager = dbManager;
        this.twitchApi = twitchApi;
        this.scheduler = scheduler;

        streamData = null;
    }

    public void start() {

        scheduledFuture = scheduler.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                Stream stream;
                try {
                    stream = twitchApi.getStream(twitchApi.getStreamerUser().getLogin());
                } catch (HystrixRuntimeException e) {
                    e.printStackTrace();
                    System.out.println("Error retrieving stream for StreamTracker, skipping interval");
                    return;
                }
                Chatters chatters;
                try {
                    chatters = twitchApi.getChatters();
                } catch (HystrixRuntimeException e) {
                    e.printStackTrace();
                    System.out.println("Error retrieving userlist for StreamTracker, skipping interval");
                    return;
                }
                if (stream != null) {
                    Set<String> usersOnline = new HashSet<>();
                    for (String user : chatters.getAllViewers()) {
                        if (!blacklist.contains(user)) {
                            usersOnline.add(user);
                        }
                    }
                    if (usersOnline.isEmpty()) {
                        return;
                    }
                    if (streamData == null) {
                        streamData = new StreamData(dbManager, twitchApi);
                    }
                    streamData.updateUsersMinutes(usersOnline);
                    streamData.updateViewerCounts(stream.getViewerCount());
                } else {
                    if (streamData != null) {
                        streamData.endStream();
                        ReportBuilder.generateReport(dbManager, streamData);
                        streamData = null;
                    }
                }
            }
        }, 0, INTERVAL, TimeUnit.MINUTES);
    }

    public void stop() {
        scheduledFuture.cancel(false);
        if (streamData != null) {
            streamData.endStream();
            ReportBuilder.generateReport(dbManager, streamData);
            streamData = null;
        }
    }

    //returns the length of time the given user has been watching the stream, 0 if there's no stream
    public int getViewerMinutes(String username) {
        if (streamData == null) {
            return 0;
        } else {
            return streamData.getViewerMinutes(username.toLowerCase());
        }
    }

    private Set<String> blacklistInit() {
        Set<String> blacklist = new HashSet<>();
        try {
            File file = new File(BLACKLIST_FILENAME);
            Scanner sc = new Scanner(file);
            while (sc.hasNextLine()) {
                blacklist.add(sc.nextLine());
            }
            sc.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.printf("Loaded blacklist with %d entries%n", blacklist.size());
        return blacklist;
    }
}
