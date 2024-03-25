package listeners.events;

import com.github.twitch4j.helix.domain.User;
import com.github.twitch4j.pubsub.events.ChannelBitsEvent;
import com.netflix.hystrix.exception.HystrixRuntimeException;
import listeners.TwitchEventListener;
import org.apache.commons.lang.SystemUtils;
import util.CommonUtils;
import util.FileWriter;
import util.TwitchApi;

import static java.lang.System.out;

public class RecentCheerListener implements TwitchEventListener {
    private static final String LOCAL_RECENT_CHEER_FILENAME = "recent_cheer.txt";
    
    private final TwitchApi twitchApi;
    
    public RecentCheerListener(CommonUtils commonUtils) {
        twitchApi = commonUtils.getTwitchApi();
    }
    
    @Override
    public void onCheer(ChannelBitsEvent bitsEvent) {
        bitsEvent.getData().getBitsUsed();
    
        String userId = bitsEvent.getData().getUserId();
        User user;
        try {
            user = twitchApi.getUserById(userId);
        } catch (HystrixRuntimeException e) {
            e.printStackTrace();
            out.printf(
                    "error retrieving data for bit user with id %s%n",
                    userId
            );
            user = null;
        }
        if (user == null) {
            out.println("error: bit user is null");
            return;
        }
    
        String latestCheerText = String.format("%s - %d", user.getDisplayName(), bitsEvent.getData().getBitsUsed());
        FileWriter.writeToFile(getOutputLocation(), LOCAL_RECENT_CHEER_FILENAME, latestCheerText);
    }
    
    private String getOutputLocation() {
        if (SystemUtils.IS_OS_LINUX) {
            return "/srv/goombotio/";
        }
        return "output/";
    }
}
