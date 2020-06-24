package Util;

import Database.Stats.WatchTimeDb;
import Functions.StreamData;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class ReportBuilder {
    
    private final static String REPORT_LOCATION = "streamreports/";
    
    public static void generateReport(StreamData streamData) {
        StringBuilder report = new StringBuilder();
        
        report.append("REPORT\n\n");
        report.append(generateReportStats(streamData));
        report.append("\n\n");
        report.append(generateReportAllViewers(streamData));
        report.append("\n\n");
        report.append(generateReportNewViewers(streamData));
        report.append("\n\n");
        report.append(generateReportReturningViewers(streamData));
        
        String filename = getReportFilename();
        
        FileWriter.writeToFile(REPORT_LOCATION, filename, report.toString());
        System.out.println(String.format("Output report to %s%s", REPORT_LOCATION, filename));
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
    
    private static String generateReportAllViewers(StreamData streamData) {
        WatchTimeDb watchTimeDb = WatchTimeDb.getInstance();
        StringBuilder allViewersReport = new StringBuilder();
        
        int allWatchTime = 0;
        ArrayList<Map.Entry<String, Integer>> biggestViewers = streamData.getTopFollowerCounts();
        
        allViewersReport.append("------ All Viewers ------\n");
        allViewersReport.append("Biggest Viewers:\n");
        
        int maxIndex = 0;
        int maxNameLength = 0;
        int maxFollowers = 0;
        int maxMinutes = 0;
        for (int i = 0; i < 10 && i < biggestViewers.size(); i++) {
            String name = biggestViewers.get(i).getKey();
            maxIndex = i + 1;
            maxNameLength = Math.max(maxNameLength, name.length());
            maxFollowers = Math.max(maxFollowers, biggestViewers.get(i).getValue());
            maxMinutes = Math.max(maxMinutes, streamData.getViewerMinutes(name));
        }
        for (int i = 0; i < 10 && i < biggestViewers.size(); i++) {
            int index = i + 1;
            String name = biggestViewers.get(i).getKey();
            int followers = biggestViewers.get(i).getValue();
            int minutes = streamData.getViewerMinutes(name);
            allViewersReport.append(buildPaddedBiggestViewersString(index, name, followers, minutes,
                    maxIndex, maxNameLength, maxFollowers, maxMinutes));
        }
        
        allViewersReport.append("\n");
        
        HashMap<String, Integer> usersMap = streamData.getAllViewerMinutes();
        for (Integer value : usersMap.values()) {
            allWatchTime += value / (60 * 1000);
        }
        int averageAllMinutes = 0;
        if (usersMap.size() != 0) {
            averageAllMinutes = allWatchTime / usersMap.size();
        }
        int averageWatchPercent = (int)((float)averageAllMinutes / streamData.getStreamLength() * 100);
        
        int totalAge = 0;
        int weightedAgeNumer = 0;
        int weightedAgeDenom = 0;
        for(Map.Entry<String, Integer> entry : usersMap.entrySet()) {
            String name = entry.getKey();
            int minutes = entry.getValue() / 1000;
            Date firstSeen = watchTimeDb.getFirstSeen(name);
            int ageDays = Math.toIntExact(TimeUnit.DAYS.convert(getDate().getTime() - firstSeen.getTime(), TimeUnit.MILLISECONDS));
            
            totalAge += ageDays;
            weightedAgeNumer += ageDays * minutes;
            weightedAgeDenom += minutes;
        }
        int averageAge = totalAge / usersMap.size();
        int weightedAge = weightedAgeNumer / weightedAgeDenom;
        
        allViewersReport.append(String.format("Total Viewers:       %d viewers\n", usersMap.size()));
        allViewersReport.append(String.format("Average Watchtime:   %d minutes\n", averageAllMinutes));
        allViewersReport.append(String.format("Average Watch%%:      %d%%\n", averageWatchPercent));
        allViewersReport.append(String.format("Average Viewer Age:  %d days\n", averageAge));
        allViewersReport.append(String.format("Weighted Viewer Age: %d days\n", weightedAge));
        
        return allViewersReport.toString();
    }
    
    private static String buildPaddedBiggestViewersString(int index, String name, int followers, int minutes,
                                                         int maxIndex, int maxNameLength, int maxFollowers, int maxMinutes) {
        StringBuilder output = new StringBuilder();
        int indexPadding = ((int)Math.log10(maxIndex) + 1) - ((int)Math.log10(index) + 1);
        int namePadding = maxNameLength - name.length();
        int followersPadding = ((int)Math.log10(maxFollowers) + 1) - ((int)Math.log10(followers) + 1);
        int minutesPadding = ((int)Math.log10(maxMinutes) + 1) - ((int)Math.log10(minutes) + 1);
        
        output.append(index);
        output.append(". ");
        for (int i = 0; i < indexPadding; i++) {
            output.append(' ');
        }
        output.append(name);
        output.append(": ");
        for (int i = 0; i < namePadding; i++) {
            output.append(' ');
        }
        for (int i = 0; i < followersPadding; i++) {
            output.append(' ');
        }
        output.append(followers);
        output.append(" followers, ");
        for (int i = 0; i < minutesPadding; i++) {
            output.append(' ');
        }
        output.append(minutes);
        output.append(" minutes\n");
        return output.toString();
    }
    
    private static String generateReportNewViewers(StreamData streamData) {
        StringBuilder newViewersReport = new StringBuilder();
    
        ArrayList<Map.Entry<String, Integer>> newViewersList = streamData.getOrderedWatchtimeList(streamData.getNewViewers());
        int newWatchTime = 0;
        
        newViewersReport.append("------ New Viewers ------\n");
    
        int maxNameLength = 0;
        int maxMinutes = 0;
        for (Map.Entry<String, Integer> viewer : newViewersList) {
            maxNameLength = Math.max(maxNameLength, viewer.getKey().length());
            maxMinutes = Math.max(maxMinutes, viewer.getValue() / (60 * 1000));
        }
        for (Map.Entry<String, Integer> viewer : newViewersList) {
            int minutes = viewer.getValue() / (60 * 1000);
            newWatchTime += minutes;
            newViewersReport.append(buildPaddedViewerMinutesString(viewer.getKey(), minutes, maxNameLength, maxMinutes));
        }
        newViewersReport.append("\n");
        
        int averageNewMinutes = 0;
        if (newViewersList.size() != 0) {
            averageNewMinutes = newWatchTime / newViewersList.size();
        }
        
        int averageWatchPercent = (int)((float)averageNewMinutes / streamData.getStreamLength() * 100);
        
        newViewersReport.append(String.format("Total New Viewers: %d viewers\n", newViewersList.size()));
        newViewersReport.append(String.format("Average Watchtime: %d minutes\n", averageNewMinutes));
        newViewersReport.append(String.format("Average Watch%%:    %d%%\n", averageWatchPercent));
    
        return newViewersReport.toString();
    }
    
    private static String generateReportReturningViewers(StreamData streamData) {
        StringBuilder returningViewersReport = new StringBuilder();
        
        ArrayList<Map.Entry<String, Integer>> returningViewersList = streamData.getOrderedWatchtimeList(streamData.getReturningViewers());
        int returningWatchTime = 0;
        returningViewersReport.append("------ Returning Viewers ------\n");
        
        int maxNameLength = 0;
        int maxMinutes = 0;
        for (Map.Entry<String, Integer> viewer : returningViewersList) {
            maxNameLength = Math.max(maxNameLength, viewer.getKey().length());
            maxMinutes = Math.max(maxMinutes, viewer.getValue() / (60 * 1000));
        }
        for (Map.Entry<String, Integer> viewer : returningViewersList) {
            int minutes = viewer.getValue() / (60 * 1000);
            returningWatchTime += minutes;
            returningViewersReport.append(buildPaddedViewerMinutesString(viewer.getKey(), minutes, maxNameLength, maxMinutes));
        }
        returningViewersReport.append("\n");
    
        int averageReturningMinutes = 0;
        if (returningViewersList.size() != 0) {
            averageReturningMinutes = returningWatchTime / returningViewersList.size();
        }
        
        int averageWatchPercent = (int)((float)averageReturningMinutes / streamData.getStreamLength() * 100);
        
        returningViewersReport.append(String.format("Total Returning Viewers: %d viewers\n", returningViewersList.size()));
        returningViewersReport.append(String.format("Average Watchtime:       %d minutes\n", averageReturningMinutes));
        returningViewersReport.append(String.format("Average Watch%%:          %d%%\n", averageWatchPercent));
        
        return returningViewersReport.toString();
    }
    
    private static String buildPaddedViewerMinutesString(String name, int minutes, int maxNameLength, int maxMinutes) {
        StringBuilder output = new StringBuilder();
        int namePadding = maxNameLength - name.length();
        int minutesPadding = ((int)Math.log10(maxMinutes) + 1) - ((int)Math.log10(minutes) + 1);
        output.append(name);
        output.append(": ");
        for (int i = 0; i < namePadding; i++) {
            output.append(' ');
        }
        for (int i = 0; i < minutesPadding; i++) {
            output.append(' ');
        }
        output.append(minutes);
        output.append(" minutes\n");
        return output.toString();
    }
    
    private static String getReportFilename() {
        LocalDateTime date = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH:mm", Locale.ENGLISH);
        return  "StreamReport" + formatter.format(date) + ".txt";
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
