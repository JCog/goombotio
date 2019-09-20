package Util;

import Functions.StatsTracker;
import Functions.StreamInfo;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class ReportBuilder {
    
    private final static String REPORT_LOCATION = "streamreports/";
    
    public static void generateReport(StreamInfo streamInfo, StatsTracker statsTracker) {
        StringBuilder report = new StringBuilder();
        HashMap<String, Integer> usersMap = statsTracker.getUsersMapCopy();
        
        report.append("REPORT\n\n");
        report.append(generateReportStats(streamInfo));
        report.append("\n\n");
        report.append(generateReportAllViewers(statsTracker, usersMap));
        report.append("\n\n");
        report.append(generateReportNewViewers(statsTracker));
        report.append("\n\n");
        report.append(generateReportReturningViewers(statsTracker));
        
        String filename = getReportFilename();
        
        FileWriter.writeToFile(REPORT_LOCATION, filename, report.toString());
    }
    
    private static String generateReportStats(StreamInfo streamInfo) {
        StringBuilder streamStatsReport = new StringBuilder();
    
        int averageViewers = streamInfo.getAverageViewers();
        int medianViewers = streamInfo.getMedianViewers();
        int maxViewers = streamInfo.getMaxViewers();
    
        streamStatsReport.append("------ Stream Stats ------\n");
        streamStatsReport.append(String.format("Average Viewer Count: %d\n", averageViewers));
        streamStatsReport.append(String.format("Median Viewer Count:  %d\n", medianViewers));
        streamStatsReport.append(String.format("Max Viewer Count:     %d\n", maxViewers));
        
        return streamStatsReport.toString();
    }
    
    private static String generateReportAllViewers(StatsTracker statsTracker, HashMap<String, Integer> usersMap) {
        StringBuilder allViewersReport = new StringBuilder();
        
        int allWatchTime = 0;
        ArrayList<Map.Entry<String, Integer>> biggestViewers = statsTracker.getTopFollowerCounts();
        
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
            maxMinutes = Math.max(maxMinutes, statsTracker.getViewerMinutes(name));
        }
        for (int i = 0; i < 10 && i < biggestViewers.size(); i++) {
            int index = i + 1;
            String name = biggestViewers.get(i).getKey();
            int followers = biggestViewers.get(i).getValue();
            int minutes = statsTracker.getViewerMinutes(name);
            allViewersReport.append(buildPaddedBiggestViewersString(index, name, followers, minutes,
                    maxIndex, maxNameLength, maxFollowers, maxMinutes));
        }
        
        allViewersReport.append("\n");
        
        for (Integer value : usersMap.values()) {
            allWatchTime += value / (60 * 1000);
        }
        int averageAllMinutes = 0;
        if (usersMap.size() != 0) {
            averageAllMinutes = allWatchTime / usersMap.size();
        }
        allViewersReport.append(String.format("Total Viewers:     %d viewers\n", usersMap.size()));
        allViewersReport.append(String.format("Average Watchtime: %d minutes\n", averageAllMinutes));
        
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
    
    private static String generateReportNewViewers(StatsTracker statsTracker) {
        StringBuilder newViewersReport = new StringBuilder();
    
        ArrayList<Map.Entry<String, Integer>> newViewersList = statsTracker.getNewViewers();
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
        //New Followers
        Set<String> newFollowers = statsTracker.getFollowers();
        newFollowers.removeAll(statsTracker.getStartingFollowers());
        newViewersReport.append("New Followers:\n");
        for (String follower : newFollowers) {
            newViewersReport.append(String.format("%s\n", follower));
        }
        newViewersReport.append("\n");
        
        int averageNewMinutes = 0;
        if (newViewersList.size() != 0) {
            averageNewMinutes = newWatchTime / newViewersList.size();
        }
        newViewersReport.append(String.format("Total New Viewers: %d viewers\n", newViewersList.size()));
        newViewersReport.append(String.format("Average Watchtime: %d minutes\n", averageNewMinutes));
    
        return newViewersReport.toString();
    }
    
    private static String generateReportReturningViewers(StatsTracker statsTracker) {
        StringBuilder returningViewersReport = new StringBuilder();
        
        ArrayList<Map.Entry<String, Integer>> returningViewersList = statsTracker.getReturningViewers();
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
        
        returningViewersReport.append(String.format("Total Returning Viewers: %d viewers\n", returningViewersList.size()));
        returningViewersReport.append(String.format("Average Watchtime:       %d minutes\n", averageReturningMinutes));
        
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
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ENGLISH);
        return  "StreamReport" + formatter.format(date) + ".txt";
    }
}
