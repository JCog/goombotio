package Functions;

import Util.FileWriter;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;

import static java.lang.System.*;

public class SubPointUpdater {
    private static final String STREAMLABS_SUB_POINTS_FILENAME = "src/main/resources/sub_points.txt";
    private static final String LOCAL_SUB_POINTS_FILE_LOCATION = "output/";
    private static final String LOCAL_SUB_POINTS_FILENAME = "sub_points.txt";
    private static final int PERMANENT_SUB_COUNT = 2;
    private static final int INTERVAL = 60 * 1000;
    private static final int TIER_2_MULTIPLIER = 2;
    private static final int TIER_3_MULTIPLIER = 6;
    
    private String streamLabsFilename;
    private String displayFormat;
    private int tier2Subs;
    private int tier3Subs;
    private Timer timer;
    private int subPoints;
    
    public SubPointUpdater() {
        timer = new Timer();
        subPoints = 0;
    }
    
    public void start() {
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                updateSubTierCounts();
                outputSubPointsFile();
            }
        }, 0, INTERVAL);
    }
    
    public void stop() {
        timer.cancel();
    }
    
    private void updateSubTierCounts() {
        String tier2String;
        String tier3String;
    
        try {
            File file = new File(STREAMLABS_SUB_POINTS_FILENAME);
            Scanner sc = new Scanner(file);
            streamLabsFilename = sc.nextLine();
            displayFormat = sc.nextLine();
            tier2String = sc.nextLine().split(" ")[1];
            tier3String = sc.nextLine().split(" ")[1];
            sc.close();
        }
        catch (FileNotFoundException e) {
            out.println(String.format("Unable to find file \"%s\", defaulting to 0", STREAMLABS_SUB_POINTS_FILENAME));
            e.printStackTrace();
            tier2String = "0";
            tier3String = "0";
        }
    
        try {
            tier2Subs = Integer.parseInt(tier2String);
            tier3Subs = Integer.parseInt(tier3String);
        }
        catch (NumberFormatException e) {
            out.println("Error parsing sub counts from file, defaulting to 0");
            e.printStackTrace();
            tier2Subs = 0;
            tier3Subs = 0;
        }
    }
    
    private void outputSubPointsFile() {
        int newSubPoints = getStreamlabsSubScore();
        newSubPoints += tier2Subs * TIER_2_MULTIPLIER - tier2Subs;
        newSubPoints += tier3Subs * TIER_3_MULTIPLIER - tier3Subs;
    
        if (newSubPoints != subPoints) {
            subPoints = newSubPoints;
            FileWriter.writeToFile(
                    LOCAL_SUB_POINTS_FILE_LOCATION,
                    LOCAL_SUB_POINTS_FILENAME,
                    String.format(displayFormat, newSubPoints));
        }
    }
    
    private int getStreamlabsSubScore() {
        int subScore = 1;
        try {
            File file = new File(streamLabsFilename);
            Scanner sc = new Scanner(file);
            subScore = sc.nextInt();
            sc.close();
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        catch (Exception e) {
            out.println(String.format("Unable to find sub score in file \"%s\"", streamLabsFilename));
            e.printStackTrace();
        }
        
        //subtract permanent subs because the twitch api counts them even though they don't count toward sub score
        return subScore - PERMANENT_SUB_COUNT;
    }
}
