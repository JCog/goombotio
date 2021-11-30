package functions;

import com.github.twitch4j.helix.domain.User;
import com.netflix.hystrix.exception.HystrixRuntimeException;
import org.apache.commons.lang.SystemUtils;
import util.FileWriter;
import util.Settings;
import util.TwitchApi;

import java.util.Timer;
import java.util.TimerTask;

import static java.lang.System.out;

public class SubPointUpdater {
    private static final String LOCAL_SUB_POINTS_FILENAME = "sub_points.txt";
    private static final String SUB_GOAL_COMMAND = "!subgoal";
    private static final int INTERVAL = 60 * 1000;
    
    private final Timer timer = new Timer();
    private final User streamerUser;
    private final TwitchApi twitchApi;
    private final Settings settings;
    
    private int subPoints;
    
    public SubPointUpdater(TwitchApi twitchApi, User streamerUser, Settings settings) {
        this.twitchApi = twitchApi;
        this.streamerUser = streamerUser;
        this.settings = settings;
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
        try {
            subPoints = twitchApi.getSubPoints(streamerUser.getId());
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
