package functions;

import com.github.twitch4j.helix.domain.Stream;
import com.github.twitch4j.helix.domain.User;
import com.jcog.utils.TwitchApi;
import com.jcog.utils.database.DbManager;
import com.netflix.hystrix.exception.HystrixRuntimeException;
import listeners.events.CloudListener;
import util.ReportBuilder;
import util.TwirkInterface;

import java.io.File;
import java.util.HashSet;
import java.util.Scanner;
import java.util.TimerTask;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class StreamTracker {
    private static final String BLACKLIST_FILENAME = "blacklist.txt";
    private static final int INTERVAL = 1; //minutes

    private final HashSet<String> blacklist = blacklistInit();

    private final TwirkInterface twirk;
    private final DbManager dbManager;
    private final TwitchApi twitchApi;
    private final User streamerUser;
    private final ScheduledExecutorService scheduler;
    private final CloudListener cloudListener;

    private StreamData streamData;
    private ScheduledFuture<?> scheduledFuture;

    public StreamTracker(TwirkInterface twirk,
                         DbManager dbManager,
                         TwitchApi twitchApi,
                         User streamerUser,
                         ScheduledExecutorService scheduler,
                         CloudListener cloudListener
    ) {
        this.twirk = twirk;
        this.dbManager = dbManager;
        this.twitchApi = twitchApi;
        this.streamerUser = streamerUser;
        this.scheduler = scheduler;
        this.cloudListener = cloudListener;

        streamData = null;
    }

    public void start() {

        scheduledFuture = scheduler.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                Stream stream;
                try {
                    stream = twitchApi.getStream();
                }
                catch (HystrixRuntimeException e) {
                    e.printStackTrace();
                    System.out.println("Error retrieving stream for StreamTracker, skipping interval");
                    return;
                }
                if (stream != null) {
                    HashSet<String> usersOnline = new HashSet<>();
                    for (String user : twirk.getUsersOnline()) {
                        if (!blacklist.contains(user)) {
                            usersOnline.add(user);
                        }
                    }
                    if (usersOnline.isEmpty()) {
                        return;
                    }
                    if (streamData == null) {
                        streamData = new StreamData(dbManager, twitchApi, streamerUser);
                        cloudListener.reset();
                    }
                    streamData.updateUsersMinutes(usersOnline);
                    streamData.updateViewerCounts(stream.getViewerCount());
                }
                else {
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
        }
        else {
            return streamData.getViewerMinutes(username);
        }
    }

    private HashSet<String> blacklistInit() {
        HashSet<String> blacklist = new HashSet<>();
        try {
            File file = new File(BLACKLIST_FILENAME);
            Scanner sc = new Scanner(file);
            while (sc.hasNextLine()) {
                blacklist.add(sc.nextLine());
            }
            sc.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println(String.format("Loaded blacklist with %d entries", blacklist.size()));
        return blacklist;
    }
}
