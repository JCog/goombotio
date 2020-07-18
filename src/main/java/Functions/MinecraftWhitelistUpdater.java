package Functions;

import Database.Entries.MinecraftUser;
import Database.Misc.MinecraftUserDb;
import Util.FileWriter;
import Util.Settings;
import Util.TwitchApi;
import com.github.twitch4j.helix.domain.Subscription;
import com.github.twitch4j.helix.domain.User;
import com.jcraft.jsch.*;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MinecraftWhitelistUpdater {
    private static final String FILENAME = "whitelist.json";
    private static final int INTERVAL = 60 * 1000;

    private final JSch jsch = new JSch();
    private final MinecraftUserDb minecraftUserDb = MinecraftUserDb.getInstance();
    private final Timer timer = new Timer();
    private final TwitchApi twitchApi;
    private final User streamerUser;

    public MinecraftWhitelistUpdater(TwitchApi twitchApi, User streamerUser) {
        this.twitchApi = twitchApi;
        this.streamerUser = streamerUser;
    }

    public void start() {
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                JSONArray currentWhitelist = readInWhitelist();
                JSONArray newWhitelist = createWhitelist();
                if (!whitelistsEqual(currentWhitelist, newWhitelist)) {
                    updateLocalWhitelist(newWhitelist);
                    updateRemoteWhitelist();
                }
            }
        }, 0, INTERVAL);
    }

    public void stop() {
        timer.cancel();
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

    private JSONArray createWhitelist() {
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
        FileWriter.writeToFile("", FILENAME, whitelist.toJSONString());
    }

    private void updateRemoteWhitelist() {
        String server = Settings.getMinecraftServer();
        String user = Settings.getMinecraftUser();
        String password = Settings.getMinecraftPassword();
        String whitelistLocation = Settings.getMinecraftWhitelistLocation();

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
