package listeners.events;

import com.github.twitch4j.eventsub.events.ChannelCheerEvent;
import com.github.twitch4j.helix.domain.User;
import com.netflix.hystrix.exception.HystrixRuntimeException;
import listeners.TwitchEventListener;
import org.apache.commons.lang.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.CommonUtils;
import util.FileWriter;
import util.TwitchApi;

public class RecentCheerListener implements TwitchEventListener {
    private static final Logger log = LoggerFactory.getLogger(RecentCheerListener.class);
    private static final String LOCAL_RECENT_CHEER_FILENAME = "recent_cheer.txt";

    private final TwitchApi twitchApi;
    
    public RecentCheerListener(CommonUtils commonUtils) {
        twitchApi = commonUtils.twitchApi();
    }
    
    @Override
    public void onCheer(ChannelCheerEvent cheerEvent) {
        String userId = cheerEvent.getUserId();
        User user;
        try {
            user = twitchApi.getUserById(userId);
        } catch (HystrixRuntimeException e) {
            log.error("error retrieving data for bit user with id {}", userId);
            user = null;
        }
        if (user == null) {
            log.error("bit user is null");
            return;
        }
    
        String latestCheerText = String.format("%s - %d", user.getDisplayName(), cheerEvent.getBits());
        FileWriter.writeToFile(getOutputLocation(), LOCAL_RECENT_CHEER_FILENAME, latestCheerText);
    }
    
    private String getOutputLocation() {
        if (SystemUtils.IS_OS_LINUX) {
            return "/srv/goombotio/";
        }
        return "output/";
    }
}
