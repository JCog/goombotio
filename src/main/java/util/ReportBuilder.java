package util;

import com.github.twitch4j.helix.domain.User;
import database.DbManager;
import database.stats.WatchTimeDb;
import functions.StreamData;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static java.lang.System.out;

public class ReportBuilder {

    private final static String REPORT_LOCATION = "streamreports/";

    public static void generateReport(DbManager dbManager, StreamData streamData) {
        out.println("Building report...");

        String filename = getReportFilename();

        String report = "REPORT\n\n" +
                generateReportStats(streamData) +
                "\n\n" +
                generateReportAllViewers(dbManager, streamData) +
                "\n\n" +
                generateReportNewViewers(streamData) +
                "\n\n" +
                generateReportReturningViewers(streamData);
        boolean successful = FileWriter.writeToFile(REPORT_LOCATION, filename, report);
        if (successful) {
            out.printf("Report output to:\n%s%s\n", REPORT_LOCATION, filename);
        } else {
            out.println("Error writing report to file");
        }
    }

    private static String generateReportStats(StreamData streamData) {
        out.print("Generating general stats... ");
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
        
        out.println("done.");
        return streamStatsReport.toString();
    }

    private static String generateReportAllViewers(DbManager dbManager, StreamData streamData) {
        out.print("Generating All Viewer stats... ");
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
        if (userIdMinutesMap.size() != 0) {
            averageAllMinutes = allWatchTime / userIdMinutesMap.size();
        }
        int averageWatchPercent = (int) ((float) averageAllMinutes / streamData.getStreamLength() * 100);

        int totalAge = 0;
        int weightedAgeNumer = 0;
        int weightedAgeDenom = 0;
        for (Map.Entry<String,Integer> entry : userIdMinutesMap.entrySet()) {
            String userId = entry.getKey();
            int minutes = entry.getValue();
            
            Date firstSeen = watchTimeDb.getFirstSeenById(Long.parseLong(userId));
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
        
        out.println("done.");
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

    private static String generateReportNewViewers(StreamData streamData) {
        out.print("Generating New Viewer stats... ");
        StringBuilder newViewersReport = new StringBuilder();
    
        List<Map.Entry<User,Integer>> orderedNewViewerMinutesMap = streamData.getOrderedWatchtimeList(
                streamData.getNewViewers()
        );
        int newWatchTime = 0;

        newViewersReport.append("------ New Viewers ------\n");

        int maxNameLength = 0;
        int maxMinutes = 0;
        for (Map.Entry<User,Integer> newViewerMinutesEntry : orderedNewViewerMinutesMap) {
            maxNameLength = Math.max(maxNameLength, newViewerMinutesEntry.getKey().getDisplayName().length());
            maxMinutes = Math.max(maxMinutes, newViewerMinutesEntry.getValue());
        }
        for (Map.Entry<User,Integer> newViewerMinutesEntry : orderedNewViewerMinutesMap) {
            String username = newViewerMinutesEntry.getKey().getDisplayName();
            int minutes = newViewerMinutesEntry.getValue();
            
            newWatchTime += minutes;
            newViewersReport.append(buildPaddedViewerMinutesString(
                    username,
                    minutes,
                    maxNameLength,
                    maxMinutes
            ));
        }
        newViewersReport.append("\n");

        int averageNewMinutes = 0;
        if (orderedNewViewerMinutesMap.size() != 0) {
            averageNewMinutes = newWatchTime / orderedNewViewerMinutesMap.size();
        }

        int averageWatchPercent = (int) ((float) averageNewMinutes / streamData.getStreamLength() * 100);

        newViewersReport.append(String.format("Total New Viewers: %d viewers\n", orderedNewViewerMinutesMap.size()));
        newViewersReport.append(String.format("Average Watchtime: %d minutes\n", averageNewMinutes));
        newViewersReport.append(String.format("Average Watch%%:    %d%%\n", averageWatchPercent));
        
        out.println("done.");
        return newViewersReport.toString();
    }

    private static String generateReportReturningViewers(StreamData streamData) {
        out.print("Generating Returning Viewer stats... ");
        StringBuilder returningViewersReport = new StringBuilder();
    
        List<Map.Entry<User,Integer>> orderedReturningViewerMinutesMap = streamData.getOrderedWatchtimeList(
                streamData.getReturningViewers()
        );
        int returningWatchTime = 0;
        returningViewersReport.append("------ Returning Viewers ------\n");

        int maxNameLength = 0;
        int maxMinutes = 0;
        for (Map.Entry<User,Integer> returningViewerMinutesEntry : orderedReturningViewerMinutesMap) {
            maxNameLength = Math.max(maxNameLength, returningViewerMinutesEntry.getKey().getDisplayName().length());
            maxMinutes = Math.max(maxMinutes, returningViewerMinutesEntry.getValue());
        }
        for (Map.Entry<User,Integer> returningViewerMinutesEntry : orderedReturningViewerMinutesMap) {
            String username = returningViewerMinutesEntry.getKey().getDisplayName();
            int minutes = returningViewerMinutesEntry.getValue();
            
            returningWatchTime += minutes;
            returningViewersReport.append(buildPaddedViewerMinutesString(
                    username,
                    minutes,
                    maxNameLength,
                    maxMinutes
            ));
        }
        returningViewersReport.append("\n");

        int averageReturningMinutes = 0;
        if (orderedReturningViewerMinutesMap.size() != 0) {
            averageReturningMinutes = returningWatchTime / orderedReturningViewerMinutesMap.size();
        }

        int averageWatchPercent = (int) ((float) averageReturningMinutes / streamData.getStreamLength() * 100);

        returningViewersReport.append(String.format("Total Returning Viewers: %d viewers\n",
                                                    orderedReturningViewerMinutesMap.size()));
        returningViewersReport.append(String.format("Average Watchtime:       %d minutes\n", averageReturningMinutes));
        returningViewersReport.append(String.format("Average Watch%%:          %d%%\n", averageWatchPercent));
        
        out.println("done.");
        return returningViewersReport.toString();
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
