package dev.jcog.goombotio.util;

import com.github.twitch4j.helix.domain.OutboundFollow;
import com.github.twitch4j.helix.domain.User;
import com.netflix.hystrix.exception.HystrixRuntimeException;
import dev.jcog.goombotio.database.DbManager;
import dev.jcog.goombotio.database.stats.WatchTimeDb;
import dev.jcog.goombotio.functions.StreamData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class ReportBuilder {
    private static final Logger log = LoggerFactory.getLogger(ReportBuilder.class);
    private final static String REPORT_LOCATION = "streamreports/";

    public static void generateReport(CommonUtils commonUtils, StreamData streamData) {
        log.info("Building report...");

        String filename = getReportFilename();
        TwitchApi twitchApi = commonUtils.twitchApi();
        final List<String> followIds = new ArrayList<>();
        try {
            followIds.addAll(
                    twitchApi.getFollowedChannels(twitchApi.getStreamerUser().getId()).stream()
                            .map(OutboundFollow::getBroadcasterId)
                            .toList());
        } catch (HystrixRuntimeException e) {
            log.error(e.getMessage());
        }
        List<User> followedViewers = streamData.getAllViewers().stream().filter(user -> followIds.contains(user.getId())).toList();

        String report =
                "REPORT\n\n" +
                generateReportStats(streamData) +
                "\n\n" +
                generateReportAllViewers(commonUtils.dbManager(), streamData) +
                "\n\n" +
                generateReportViewers(streamData, followedViewers, "Followed") +
                "\n\n" +
                generateReportViewers(streamData, streamData.getNewViewers(), "New") +
                "\n\n" +
                generateReportViewers(streamData, streamData.getReturningViewers(), "Returning");
        boolean successful = FileWriter.writeToFile(REPORT_LOCATION, filename, report);
        if (successful) {
            log.info("Report output to:{}{}", REPORT_LOCATION, filename);
        } else {
            log.error("Error writing report to file");
        }
    }

    private static String generateReportStats(StreamData streamData) {
        StringBuilder streamStatsReport = new StringBuilder();

        int streamLength = streamData.getStreamLength();
        int averageViewers = streamData.getAverageViewers();
        int medianViewers = streamData.getMedianViewers();
        int maxViewers = streamData.getMaxViewers();

        streamStatsReport.append("------ Stream Stats ------\n");
        streamStatsReport.append(String.format("Stream Length:        %d minutes\n", streamLength));
        streamStatsReport.append(String.format("Average Viewer Count: %d\n", averageViewers));
        streamStatsReport.append(String.format("Median Viewer Count:  %d\n", medianViewers));
        streamStatsReport.append(String.format("Max Viewer Count:     %d\n", maxViewers));

        return streamStatsReport.toString();
    }

    private static String generateReportAllViewers(DbManager dbManager, StreamData streamData) {
        WatchTimeDb watchTimeDb = dbManager.getWatchTimeDb();
        StringBuilder allViewersReport = new StringBuilder();

        int allWatchTime = 0;
        List<Map.Entry<User,Integer>> orderedViewerFollowerCountMap = streamData.getTopFollowerCounts();

        allViewersReport.append("------ All Viewers ------\n");
        allViewersReport.append("Biggest Viewers:\n");

        int maxIndex = 0;
        int maxNameLength = 0;
        int maxFollowers = 0;
        int maxMinutes = 0;
        for (int i = 0; i < 20 && i < orderedViewerFollowerCountMap.size(); i++) {
            User user = orderedViewerFollowerCountMap.get(i).getKey();
            int followerCount = orderedViewerFollowerCountMap.get(i).getValue();
            
            String username = user.getDisplayName();
            maxIndex = i + 1;
            maxNameLength = Math.max(maxNameLength, username.length());
            maxFollowers = Math.max(maxFollowers, followerCount);
            maxMinutes = Math.max(maxMinutes, streamData.getViewerMinutesById(user.getId()));
        }
        for (int i = 0; i < 10 && i < orderedViewerFollowerCountMap.size(); i++) {
            int index = i + 1;
            User user = orderedViewerFollowerCountMap.get(i).getKey();
            int followerCount = orderedViewerFollowerCountMap.get(i).getValue();
            
            String name = user.getDisplayName();
            int minutes = streamData.getViewerMinutesById(user.getId());
            allViewersReport.append(buildPaddedBiggestViewersString(
                    index,
                    name,
                    followerCount,
                    minutes,
                    maxIndex,
                    maxNameLength,
                    maxFollowers,
                    maxMinutes
            ));
        }

        allViewersReport.append("\n");

        Map<String,Integer> userIdMinutesMap = streamData.getAllViewerMinutesById();
        for (Integer minutes : userIdMinutesMap.values()) {
            allWatchTime += minutes;
        }
        int averageAllMinutes = 0;
        if (!userIdMinutesMap.isEmpty()) {
            averageAllMinutes = allWatchTime / userIdMinutesMap.size();
        }
        int averageWatchPercent = (int) ((float) averageAllMinutes / streamData.getStreamLength() * 100);

        int totalAge = 0;
        int weightedAgeNumer = 0;
        int weightedAgeDenom = 0;
        for (Map.Entry<String,Integer> entry : userIdMinutesMap.entrySet()) {
            String userId = entry.getKey();
            int minutes = entry.getValue();
            
            Date firstSeen = watchTimeDb.getFirstSeenById(userId);
            if (firstSeen == null) {
                firstSeen = getDate();
            }
            int ageDays = Math.toIntExact(TimeUnit.DAYS.convert(
                    getDate().getTime() - firstSeen.getTime(),
                    TimeUnit.MILLISECONDS
            ));

            totalAge += ageDays;
            weightedAgeNumer += ageDays * minutes;
            weightedAgeDenom += minutes;
        }
        int averageAge = totalAge / userIdMinutesMap.size();
        int weightedAge = weightedAgeNumer / weightedAgeDenom;

        allViewersReport.append(String.format("Total Viewers:       %d viewers\n", userIdMinutesMap.size()));
        allViewersReport.append(String.format("Average Watchtime:   %d minutes\n", averageAllMinutes));
        allViewersReport.append(String.format("Average Watch%%:      %d%%\n", averageWatchPercent));
        allViewersReport.append(String.format("Average Viewer Age:  %d days\n", averageAge));
        allViewersReport.append(String.format("Weighted Viewer Age: %d days\n", weightedAge));

        return allViewersReport.toString();
    }

    private static String buildPaddedBiggestViewersString(
            int index,
            String name,
            int followers,
            int minutes,
            int maxIndex,
            int maxNameLength,
            int maxFollowers,
            int maxMinutes
    ) {
        StringBuilder output = new StringBuilder();
        int indexPadding = ((int) Math.log10(maxIndex) + 1) - ((int) Math.log10(index) + 1);
        int namePadding = maxNameLength - name.length();
        int followersPadding = ((int) Math.log10(maxFollowers) + 1) - ((int) Math.log10(followers) + 1);
        int minutesPadding = ((int) Math.log10(maxMinutes) + 1) - ((int) Math.log10(minutes) + 1);

        output.append(index);
        output.append(". ");
        output.append(" ".repeat(Math.max(0, indexPadding)));
        output.append(name);
        output.append(": ");
        output.append(" ".repeat(Math.max(0, namePadding)));
        output.append(" ".repeat(Math.max(0, followersPadding)));
        output.append(followers);
        output.append(" followers, ");
        output.append(" ".repeat(Math.max(0, minutesPadding)));
        output.append(minutes);
        output.append(" minutes\n");
        return output.toString();
    }

    private static String generateReportViewers(
            StreamData streamData,
            List<User> userList,
            String viewerType
    ) {
        StringBuilder report = new StringBuilder();

        List<Map.Entry<User,Integer>> orderedViewerMinutesMap = streamData.getOrderedWatchtimeList(userList);
        int returningWatchTime = 0;
        report.append(String.format("------ %s Viewers ------%n", viewerType));

        int maxNameLength = 0;
        int maxMinutes = 0;
        for (Map.Entry<User,Integer> returningViewerMinutesEntry : orderedViewerMinutesMap) {
            maxNameLength = Math.max(maxNameLength, returningViewerMinutesEntry.getKey().getDisplayName().length());
            maxMinutes = Math.max(maxMinutes, returningViewerMinutesEntry.getValue());
        }
        for (Map.Entry<User,Integer> returningViewerMinutesEntry : orderedViewerMinutesMap) {
            String username = returningViewerMinutesEntry.getKey().getDisplayName();
            int minutes = returningViewerMinutesEntry.getValue();

            returningWatchTime += minutes;
            report.append(buildPaddedViewerMinutesString(username, minutes, maxNameLength, maxMinutes));
        }
        report.append("\n");

        int averageReturningMinutes = 0;
        if (!orderedViewerMinutesMap.isEmpty()) {
            averageReturningMinutes = returningWatchTime / orderedViewerMinutesMap.size();
        }

        int averageWatchPercent = (int) ((float) averageReturningMinutes / streamData.getStreamLength() * 100);

        report.append(String.format("Total %s Viewers: %d viewers\n", viewerType, orderedViewerMinutesMap.size()));
        report.append("Average Watchtime:");
        report.append(" ".repeat(Math.max(0, viewerType.length() - 2)));
        report.append(String.format("%d minutes\n", averageReturningMinutes));
        report.append("Average Watch%%:");
        report.append(" ".repeat(Math.max(0, viewerType.length() - 5)));
        report.append(String.format("%d%%\n", averageWatchPercent));

        return report.toString();
    }

    private static String buildPaddedViewerMinutesString(String username, int minutes, int maxNameLength, int maxMinutes) {
        StringBuilder output = new StringBuilder();
        int namePadding = maxNameLength - username.length();
        int minutesPadding = ((int) Math.log10(maxMinutes) + 1) - ((int) Math.log10(minutes) + 1);
        output.append(username);
        output.append(": ");
        output.append(" ".repeat(Math.max(0, namePadding)));
        output.append(" ".repeat(Math.max(0, minutesPadding)));
        output.append(minutes);
        output.append(" minutes\n");
        return output.toString();
    }

    private static String getReportFilename() {
        LocalDateTime date = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH:mm", Locale.ENGLISH);
        return "StreamReport" + formatter.format(date) + ".txt";
    }

    private static Date getDate() {
        Calendar date = new GregorianCalendar();
        date.set(Calendar.HOUR_OF_DAY, 12);
        date.set(Calendar.MINUTE, 0);
        date.set(Calendar.SECOND, 0);
        date.set(Calendar.MILLISECOND, 0);
        return date.getTime();
    }
}
