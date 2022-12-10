package functions;

import com.github.twitch4j.helix.domain.Subscription;
import com.github.twitch4j.helix.domain.User;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.jcraft.jsch.*;
import com.netflix.hystrix.exception.HystrixRuntimeException;
import database.DbManager;
import database.entries.MinecraftUser;
import database.misc.MinecraftUserDb;
import util.FileWriter;
import util.TwitchApi;

import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class MinecraftWhitelistUpdater {
    private static final String FILENAME = "whitelist.json";
    private static final int INTERVAL = 1; //minutes

    private final Type whitelistType = new TypeToken<ArrayList<Map<String,String>>>() {
    }.getType();
    private final Gson gson = new Gson();
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
    private boolean subOnly;

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
        subOnly = true;
    }

    public void start() {
        scheduledFuture = scheduler.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                ArrayList<Map<String,String>> currentWhitelist = readInWhitelist();
                ArrayList<Map<String,String>> newWhitelist;
                try {
                    newWhitelist = createWhitelist();
                } catch (HystrixRuntimeException e) {
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
    
    public void setSubOnly(boolean subOnly) {
        this.subOnly = subOnly;
    }
    
    public boolean isSubOnly() {
        return subOnly;
    }

    private ArrayList<Map<String,String>> readInWhitelist() {
        try (FileReader reader = new FileReader(FILENAME)) {
            return gson.fromJson(reader, whitelistType);
        } catch (IOException e) {
            System.out.println("ERROR: unable to read in Minecraft whitelist");
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    private ArrayList<Map<String,String>> createWhitelist() throws HystrixRuntimeException {
        ArrayList<MinecraftUser> whitelist = new ArrayList<>();
        if (subOnly) {
            List<Subscription> subList = twitchApi.getSubList(streamerUser.getId());
            for (Subscription sub : subList) {
                MinecraftUser user = minecraftUserDb.getUser(sub.getUserId());
                if (user != null) {
                    whitelist.add(user);
                }
            }
        } else {
            whitelist.addAll(minecraftUserDb.getAllUsers());
        }

        ArrayList<Map<String,String>> whitelistJson = new ArrayList<>();
        for (MinecraftUser user : whitelist) {
            Map<String,String> obj = new HashMap<>();
            obj.put("name", user.getMcUsername());
            obj.put("uuid", user.getMcUuid());
            whitelistJson.add(obj);
        }
        return whitelistJson;
    }

    private boolean whitelistsEqual(ArrayList<Map<String,String>> first, ArrayList<Map<String,String>> second) {
        if (first.size() != second.size()) {
            return false;
        }

        for (Map<String,String> userFirst : first) {
            String firstUuid = userFirst.get("uuid");
            String firstName = userFirst.get("name");

            boolean hasPair = false;
            for (Map<String,String> userSecond : second) {
                String secondUuid = userSecond.get("uuid");
                String secondName = userSecond.get("name");
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

    private void updateLocalWhitelist(ArrayList<Map<String,String>> whitelist) {
        boolean successful = FileWriter.writeToFile("", FILENAME, gson.toJson(whitelist));
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
        } catch (JSchException | SftpException e) {
            System.out.println("ERROR: unable to update Minecraft whitelist");
            e.printStackTrace();
        }
    }
}
