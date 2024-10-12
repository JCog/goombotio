package functions;

import org.apache.commons.lang.SystemUtils;
import util.CommonUtils;
import util.FileWriter;

import java.time.Instant;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

public class Heartbeat {
    private static final String LOCAL_HEARTBEAT_FILENAME = "heartbeat.txt";
    
    public Heartbeat(CommonUtils commonUtils) {
        commonUtils.scheduler().scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                FileWriter.writeToFile(getOutputLocation(), LOCAL_HEARTBEAT_FILENAME, Instant.now().toString());
            }
        }, 0, 1, TimeUnit.MINUTES);
    }
    
    private String getOutputLocation() {
        if (SystemUtils.IS_OS_LINUX) {
            return "/srv/goombotio/";
        }
        return "output/";
    }
}
