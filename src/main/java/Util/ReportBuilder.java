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
        for (int i = 0; i < 10 && i < biggestViewers.size(); i++) {
            String name = biggestViewers.get(i).getKey();
            int followers = biggestViewers.get(i).getValue();
            int minutes = statsTracker.getViewerMinutes(name);
            allViewersReport.append(String.format("%d. %s: %d followers, %d minutes\n", i + 1, name, followers, minutes));
        }
        
        allViewersReport.append("\n");
        
        for (Integer value : usersMap.values()) {
            allWatchTime += value / (60 * 1000);
        }
        int averageAllMinutes = allWatchTime / usersMap.size();
        allViewersReport.append(String.format("Total Viewers: %d viewers\n", usersMap.size()));
        allViewersReport.append(String.format("Average Watchtime: %d minutes\n", averageAllMinutes));
        
        return allViewersReport.toString();
    }
    
    private static String generateReportNewViewers(StatsTracker statsTracker) {
        StringBuilder newViewersReport = new StringBuilder();
    
        ArrayList<Map.Entry<String, Integer>> newViewersSet = statsTracker.getNewViewers();
        int newWatchTime = 0;
        
        newViewersReport.append("------ New Viewers ------\n");
        for (Map.Entry<String, Integer> viewer : newViewersSet) {
            int minutes = viewer.getValue() / (60 * 1000);
            newWatchTime += minutes;
            newViewersReport.append(String.format("%s: %d\n", viewer.getKey(), minutes));
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
    
        int averageNewMinutes = newWatchTime / newViewersSet.size();
        //int percentWhoFollowed = newFollowers.size() / newViewersSet.size();
        newViewersReport.append(String.format("Total New Viewers: %d viewers\n", newViewersSet.size()));
        newViewersReport.append(String.format("Average Watchtime: %d minutes\n", averageNewMinutes));
        //newViewersReport.append(String.format("Percent of New Viewers who followed: %d%%", percentWhoFollowed));
    
        return newViewersReport.toString();
    }
    
    private static String generateReportReturningViewers(StatsTracker statsTracker) {
        StringBuilder returningViewersReport = new StringBuilder();
        
        ArrayList<Map.Entry<String, Integer>> returningViewersList = statsTracker.getReturningViewers();
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
        
        return returningViewersReport.toString();
    }
    
    private static String getReportFilename() {
        LocalDateTime date = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ENGLISH);
        return  "StreamReport" + formatter.format(date) + ".txt";
    }
}
