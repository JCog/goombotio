package functions;

import com.netflix.hystrix.exception.HystrixRuntimeException;
import org.apache.commons.lang.SystemUtils;
import util.FileWriter;
import util.Settings;
import util.TwitchApi;

import java.util.TimerTask;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static java.lang.System.out;

public class SubPointUpdater {
    private static final String LOCAL_SUB_POINTS_FILENAME = "sub_points.txt";
    private static final String SUB_GOAL_COMMAND = "!subgoal";
    private static final int INTERVAL = 1; //minutes
    
    private final TwitchApi twitchApi;
    private final Settings settings;
    
    private int subPoints;
    
    public SubPointUpdater(TwitchApi twitchApi, Settings settings, ScheduledExecutorService scheduler) {
        this.twitchApi = twitchApi;
        this.settings = settings;
        subPoints = 0;
        
        init(scheduler);
    }
    
    private void init(ScheduledExecutorService scheduler) {
        scheduler.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                updateSubTierCounts();
                outputSubPointsFile();
            }
        }, 0, INTERVAL, TimeUnit.MINUTES);
    }
    
    private void updateSubTierCounts() {
        try {
            subPoints = twitchApi.getSubPoints(twitchApi.getStreamerUser().getId());
        } catch (HystrixRuntimeException e) {
            e.printStackTrace();
            out.println("Failed to get sub points. Will try again at next interval");
        }
    }
    
    private void outputSubPointsFile() {
        FileWriter.writeToFile(
                getOutputLocation(),
                LOCAL_SUB_POINTS_FILENAME,
                String.format(settings.getSubCountFormat(), subPoints)
        );
    }
    
    private void outputSubGoalCommandToFile() {
        FileWriter.writeToFile(
                getOutputLocation(),
                LOCAL_SUB_POINTS_FILENAME,
                SUB_GOAL_COMMAND
        );
    }
    
    private String getOutputLocation() {
        if (SystemUtils.IS_OS_LINUX) {
            return "/srv/goombotio/";
        }
        return "output/";
    }
}
