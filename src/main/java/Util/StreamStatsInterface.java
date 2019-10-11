package Util;

import Functions.StatsTracker;
import Functions.StreamInfo;
import Util.Database.StreamStatsDb;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class StreamStatsInterface {
    private StreamStatsDb streamStatsDb;
    
    /**
     * Interface for saving and reading information about streams in a database
     */
    public StreamStatsInterface(){
        streamStatsDb = StreamStatsDb.getInstance();
    }
    
    /**
     * Stores relevant stats about the provided stream in a database collection
     * @param streamInfo
     * @param statsTracker
     */
    public static void saveStreamStats(StreamInfo streamInfo, StatsTracker statsTracker) {
        Date startTime = streamInfo.getStartTime();
        Date endTime = streamInfo.getEndTime();
        List<Integer> viewerCounts = streamInfo.getViewerCounts();
        HashMap<String, Integer> userMinutesMap = statsTracker.getUsersMapCopy();
        
        StreamStatsDb.getInstance().addStream(startTime, endTime, viewerCounts, userMinutesMap);
    }
    
    /**
     * Returns the average concurrent viewers watching the stream during the most recent stream.
     * @return average viewer count
     */
    public int getAverageViewers() {
        List<Integer> viewerCounts = streamStatsDb.getViewerCounts();
        int sum = 0;
        for (Integer count : viewerCounts) {
            sum += count;
        }
        if (viewerCounts.size() == 0) {
            return 0;
        }
        return sum / viewerCounts.size();
    }
    
    /**
     * Returns the median concurrent viewers watching the stream during the most recent stream.
     * @return median viewer count
     */
    public int getMedianViewers() {
        List<Integer> viewerCounts = streamStatsDb.getViewerCounts();
        ArrayList<Integer> viewersCounts = new ArrayList<>(viewerCounts);
        Collections.sort(viewersCounts);
        boolean isEven = viewersCounts.size() % 2 == 0;
        int middleIndex = viewersCounts.size() / 2;
        
        if (viewersCounts.size() == 0) {
            return 0;
        }
        else if (isEven) {
            int first = viewersCounts.get(middleIndex - 1);
            int second = viewersCounts.get(middleIndex);
            return (first + second) / 2;
        }
        else {
            return viewersCounts.get(middleIndex);
        }
    }
    
    /**
     * Returns the maximum concurrent viewers watching the stream during the most recent stream.
     * @return average viewer count
     */
    public int getMaxViewers() {
        List<Integer> viewerCounts = streamStatsDb.getViewerCounts();
        int max = 0;
        for (Integer count : viewerCounts) {
            max = Math.max(max, count);
        }
        return max;
    }
    
    /**
     * Returns the time the most recent stream started
     * @return start time
     */
    public Date getStartTime() {
        return streamStatsDb.getStreamStartTime();
    }
    
    /**
     * Returns the time the most recent stream ended
     * @return end time
     */
    public Date getEndTime() {
        return streamStatsDb.getStreamEndTime();
    }
    
    /**
     * Returns the time since the start of the stream in minutes
     * @return stream length in minutes
     */
    public int getStreamLength() {
        Date startTime = streamStatsDb.getStreamStartTime();
        Date endTime = streamStatsDb.getStreamEndTime();
        long duration  = endTime.getTime() - startTime.getTime();
        return Math.toIntExact(TimeUnit.MILLISECONDS.toMinutes(duration));
    }
}
