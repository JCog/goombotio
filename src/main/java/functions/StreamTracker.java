package functions;

import com.github.twitch4j.helix.domain.Chatter;
import com.github.twitch4j.helix.domain.Stream;
import com.netflix.hystrix.exception.HystrixRuntimeException;
import database.misc.StatsBlacklistDb;
import util.CommonUtils;
import util.ReportBuilder;
import util.TwitchApi;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TimerTask;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class StreamTracker {
    private static final int INTERVAL = 1; //minutes

    private final CommonUtils commonUtils;
    private final TwitchApi twitchApi;
    private final StatsBlacklistDb statsBlacklistDb;

    private StreamData streamData;

    public StreamTracker(CommonUtils commonUtils) {
        this.commonUtils = commonUtils;
        twitchApi = commonUtils.getTwitchApi();
        statsBlacklistDb = commonUtils.getDbManager().getStatsBlacklistDb();

        streamData = null;
        
        init(commonUtils.getScheduler());
    }

    private void init(ScheduledExecutorService scheduler) {
        scheduler.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                Stream stream;
                try {
                    stream = twitchApi.getStreamByUsername(twitchApi.getStreamerUser().getLogin());
                } catch (HystrixRuntimeException e) {
                    e.printStackTrace();
                    System.out.println("Error retrieving stream for StreamTracker, skipping interval");
                    return;
                }
                List<Chatter> chatters;
                try {
                    chatters = twitchApi.getChatters();
                } catch (HystrixRuntimeException e) {
                    e.printStackTrace();
                    System.out.println("Error retrieving userlist for StreamTracker, skipping interval");
                    return;
                }
                if (stream != null) {
                    Set<String> onlineUserIds = new HashSet<>();
                    Set<String> blacklistedUserIds = statsBlacklistDb.getAllIdsSet();
                    for (Chatter user : chatters) {
                        if (blacklistedUserIds.contains(user.getUserId())) {
                            continue;
                        }
                        onlineUserIds.add(user.getUserId());
                    }
                    if (onlineUserIds.isEmpty()) {
                        return;
                    }
                    if (streamData == null) {
                        streamData = new StreamData(commonUtils);
                    }
                    streamData.updateUsersMinutes(onlineUserIds);
                    streamData.updateStreamViewCount(stream.getViewerCount());
                } else {
                    if (streamData != null) {
                        streamData.endStream();
                        ReportBuilder.generateReport(commonUtils, streamData);
                        streamData = null;
                    }
                }
            }
        }, 0, INTERVAL, TimeUnit.MINUTES);
    }

    public void stop() {
        if (streamData == null) {
            return;
        }
        streamData.endStream();
        ReportBuilder.generateReport(commonUtils, streamData);
        streamData = null;
    }

    //returns the length of time the given user has been watching the stream, 0 if there's no stream
    public int getViewerMinutesById(String userId) {
        if (streamData == null) {
            return 0;
        }
        return streamData.getViewerMinutesById(userId);
    }
}
