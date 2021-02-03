package functions;

import com.github.twitch4j.helix.domain.Subscription;
import com.github.twitch4j.helix.domain.User;
import com.jcog.utils.TwitchApi;
import com.jcog.utils.database.DbManager;
import com.jcog.utils.database.entries.MinecraftUser;
import com.jcog.utils.database.misc.MinecraftUserDb;
import com.jcraft.jsch.*;
import com.netflix.hystrix.exception.HystrixRuntimeException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import util.FileWriter;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.TimerTask;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class MinecraftWhitelistUpdater {
    private static final String FILENAME = "whitelist.json";
    private static final int INTERVAL = 1; //minutes

    private final JSch jsch = new JSch();
    private final MinecraftUserDb minecraftUserDb;
    private final TwitchApi twitchApi;
    private final User streamerUser;
    private final ScheduledExecutorService scheduler;
    private final String server;
    private final String user;
    private final String password;
    private final String whitelistLocation;

    private ScheduledFuture<?> scheduledFuture;

    public MinecraftWhitelistUpdater(
            DbManager dbManager,
            TwitchApi twitchApi,
            User streamerUser,
            ScheduledExecutorService scheduler,
            String server,
            String user,
            String password,
            String whitelistLocation
    ) {
        this.twitchApi = twitchApi;
        this.streamerUser = streamerUser;
        this.scheduler = scheduler;
        this.server = server;
        this.user = user;
        this.password = password;
        this.whitelistLocation = whitelistLocation;
        minecraftUserDb = dbManager.getMinecraftUserDb();
    }

    public void start() {
        scheduledFuture = scheduler.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                JSONArray currentWhitelist = readInWhitelist();
                JSONArray newWhitelist;
                try {
                    newWhitelist = createWhitelist();
                }
                catch (HystrixRuntimeException e) {
                    System.out.println("ERROR: unable to fetch sub list for Minecraft whitelist. Skipping interval");
                    return;
                }
                if (!whitelistsEqual(currentWhitelist, newWhitelist)) {
                    updateLocalWhitelist(newWhitelist);
                    updateRemoteWhitelist();
                }
            }
        }, 0, INTERVAL, TimeUnit.MINUTES);
    }

    public void stop() {
        scheduledFuture.cancel(false);
    }

    private JSONArray readInWhitelist() {
        JSONParser jsonParser = new JSONParser();

        try (FileReader reader = new FileReader(FILENAME)) {
            return (JSONArray) jsonParser.parse(reader);
        }
        catch (ParseException | IOException e) {
            System.out.println("ERROR: unable to read in Minecraft whitelist");
            e.printStackTrace();
        }
        return new JSONArray();
    }

    private JSONArray createWhitelist() throws HystrixRuntimeException {
        List<Subscription> subList = twitchApi.getSubList(streamerUser.getId());
        ArrayList<MinecraftUser> whitelist = new ArrayList<>();
        for (Subscription sub : subList) {
            MinecraftUser user = minecraftUserDb.getUser(sub.getUserId());
            if (user != null) {
                whitelist.add(user);
            }
        }

        JSONArray whitelistJson = new JSONArray();
        for (MinecraftUser user : whitelist) {
            JSONObject obj = new JSONObject();
            obj.put("name", user.getMcUsername());
            obj.put("uuid", user.getMcUuid());
            whitelistJson.add(obj);
        }
        return whitelistJson;
    }

    private boolean whitelistsEqual(JSONArray first, JSONArray second) {
        if (first.size() != second.size()) {
            return false;
        }

        for (Object userFirst : first) {
            JSONObject firstObj = (JSONObject) userFirst;
            String firstUuid = firstObj.get("uuid").toString();
            String firstName = firstObj.get("name").toString();

            boolean hasPair = false;
            for (Object userSecond : second) {
                JSONObject secondObj = (JSONObject) userSecond;
                String secondUuid = secondObj.get("uuid").toString();
                String secondName = secondObj.get("name").toString();
                if (firstUuid.equals(secondUuid) && firstName.equals(secondName)) {
                    hasPair = true;
                }
            }
            if (!hasPair) {
                return false;
            }
        }
        return true;
    }

    private void updateLocalWhitelist(JSONArray whitelist) {
        boolean successful = FileWriter.writeToFile("", FILENAME, whitelist.toJSONString());
        if (!successful) {
            System.out.println("Error writing Minecraft whitelist to file");
        }
    }

    private void updateRemoteWhitelist() {
        Session session;
        try {
            session = jsch.getSession(user, server);
            java.util.Properties config = new java.util.Properties();
            config.put("StrictHostKeyChecking", "no");
            config.put("PreferredAuthentications", "publickey,keyboard-interactive,password");
            session.setConfig(config);
            session.setPassword(password);

            session.connect();

            Channel channel = session.openChannel("sftp");
            channel.connect();

            ChannelSftp sftp = (ChannelSftp) channel;
            sftp.cd(whitelistLocation);
            sftp.put(FILENAME, whitelistLocation + FILENAME);
            channel.disconnect();
            session.disconnect();
            System.out.println("Successfully updated Minecraft whitelist");
        }
        catch (JSchException | SftpException e) {
            System.out.println("ERROR: unable to update Minecraft whitelist");
            e.printStackTrace();
        }
    }
}
