package Functions;

import Util.FileWriter;
import Util.Settings;
import Util.TwitchApi;
import com.github.twitch4j.TwitchClient;
import com.github.twitch4j.helix.domain.Subscription;
import com.github.twitch4j.helix.domain.SubscriptionList;
import com.github.twitch4j.helix.domain.User;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;

import static java.lang.System.out;

public class SubPointUpdater {
    private static final String STREAMLABS_SUB_POINTS_FILENAME = "src/main/resources/sub_points.txt";
    private static final String LOCAL_SUB_POINTS_FILE_LOCATION = "output/";
    private static final String LOCAL_SUB_POINTS_FILENAME = "sub_points.txt";
    private static final int INTERVAL = 60 * 1000;
    private static final int TIER_2_MULTIPLIER = 2;
    private static final int TIER_3_MULTIPLIER = 6;
    
    private final String channelId;
    private final String botId;
    private final TwitchClient twitchClient;
    private final Timer timer = new Timer();
    
    private String displayFormat;
    private int subPoints;
    
    public SubPointUpdater(TwitchClient twitchClient, TwitchApi twitchApi, User botUser) {
        this.twitchClient = twitchClient;
        channelId = twitchApi.getUserByUsername(Settings.getTwitchStream()).getId();
        botId = botUser.getId();
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
        int tier1 = 0;
        int tier2 = 0;
        int tier3 = 0;
        
        try {
            File file = new File(STREAMLABS_SUB_POINTS_FILENAME);
            Scanner sc = new Scanner(file);
            displayFormat = sc.nextLine();
            sc.close();
        }
        catch (FileNotFoundException e) {
            out.println(String.format("Unable to find file \"%s\", defaulting to 0", STREAMLABS_SUB_POINTS_FILENAME));
            e.printStackTrace();
        }
    
        SubscriptionList tempList = getSubList(null);
        while (tempList.getSubscriptions().size() > 0) {
            for (Subscription sub : tempList.getSubscriptions()) {
                //idk wtf is up with twitch's api, but sometimes the number is just wrong ¯\_(ツ)_/¯
                if (!sub.getUserId().equals(channelId) && !sub.getUserId().equals(botId)) {
                    switch (sub.getTier()) {
                        case "1000":
                            tier1++;
                            break;
                        case "2000":
                            tier2++;
                            break;
                        case "3000":
                            tier3++;
                            break;
                    }
                }
            }
            tempList = getSubList(tempList.getPagination().getCursor());
        }
        
        subPoints = tier1 + (tier2 * TIER_2_MULTIPLIER) + (tier3 * TIER_3_MULTIPLIER);
    }
    
    private SubscriptionList getSubList(String cursor) {
        return twitchClient.getHelix().getSubscriptions(
                Settings.getTwitchChannelAuthToken(),
                channelId,
                cursor,
                null,
                100).execute();
    }
    
    private void outputSubPointsFile() {
        FileWriter.writeToFile(
                LOCAL_SUB_POINTS_FILE_LOCATION,
                LOCAL_SUB_POINTS_FILENAME,
                String.format(displayFormat, subPoints)
        );
    }
}
