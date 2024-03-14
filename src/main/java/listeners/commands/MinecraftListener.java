package listeners.commands;

import api.MinecraftApi;
import com.github.twitch4j.chat.events.channel.ChannelMessageEvent;
import database.DbManager;
import database.misc.MinecraftUserDb;
import functions.MinecraftWhitelistUpdater;
import util.TwitchApi;
import util.TwitchUserLevel.USER_LEVEL;

import java.util.List;
import java.util.concurrent.ScheduledExecutorService;

public class MinecraftListener extends CommandBase {
    private static final CommandType COMMAND_TYPE = CommandType.PREFIX_COMMAND;
    private static final USER_LEVEL MIN_USER_LEVEL = USER_LEVEL.DEFAULT;
    private static final int COOLDOWN = 0;
    private static final CooldownType COOLDOWN_TYPE = CooldownType.PER_USER;
    private static final String PATTERN = "!minecraft";
    
    private static final String GENERIC_MESSAGE = "We have a community Minecraft server! It's currently open to %s. To join, register your Minecraft username by typing \"!minecraft <username>\" in the chat and you'll automatically be added to the whitelist. Then, add a new server with the address \"minecraft.jcoggers.com\". Have fun! PunchTrees";
    private static final String SUBS_ENABLED = "Sub-only mode has been enabled for the minecraft server.";
    private static final String SUBS_DISABLED = "Sub-only mode has been disabled for the minecraft server.";

    private final MinecraftUserDb minecraftUserDb;
    private final TwitchApi twitchApi;
    private final MinecraftWhitelistUpdater mcUpdater;

    public MinecraftListener(ScheduledExecutorService scheduler, TwitchApi twitchApi, DbManager dbManager, MinecraftWhitelistUpdater mcUpdater) {
        super(scheduler, COMMAND_TYPE, MIN_USER_LEVEL, COOLDOWN, COOLDOWN_TYPE, PATTERN);
        this.twitchApi = twitchApi;
        this.mcUpdater = mcUpdater;
        minecraftUserDb = dbManager.getMinecraftUserDb();
    }

    @Override
    protected void performCommand(String command, USER_LEVEL userLevel, ChannelMessageEvent messageEvent) {
        String[] messageSplit = messageEvent.getMessage().trim().split("\\s");
        if (messageSplit.length == 1) {
            String subs;
            if (mcUpdater.isSubOnly()) {
                subs = "subs only";
            } else {
                subs = "everyone";
            }
            twitchApi.channelMessage(String.format(GENERIC_MESSAGE, subs));
            return;
        }
    
        if (userLevel == USER_LEVEL.BROADCASTER) {
            if (messageSplit[1].equals("1")) {
                mcUpdater.setSubOnly(true);
                twitchApi.channelMessage(SUBS_ENABLED);
                return;
            } else if (messageSplit[1].equals("0")) {
                mcUpdater.setSubOnly(false);
                twitchApi.channelMessage(SUBS_DISABLED);
                return;
            }
        }

        String mcUsername = messageSplit[1];
        List<String> profile = MinecraftApi.getProfile(mcUsername);
        if (profile == null) {
            twitchApi.channelMessage(String.format(
                    "@%s invalid Minecraft username \"%s\"",
                    messageEvent.getUser().getName(),
                    mcUsername
            ));
            return;
        }

        String uuid = convertStringToUuid(profile.get(0));
        String name = profile.get(1);
        minecraftUserDb.addUser(messageEvent.getUser().getName(), uuid, name);
        twitchApi.channelMessage(String.format(
                "@%s Minecraft username updated to \"%s\"",
                messageEvent.getUser().getName(),
                name
        ));
    }

    //assumes correct minecraft api output -- 8-4-4-4-12
    private String convertStringToUuid(String input) {
        String[] uuid = new String[5];
        uuid[0] = input.substring(0, 8);
        uuid[1] = input.substring(8, 12);
        uuid[2] = input.substring(12, 16);
        uuid[3] = input.substring(16, 20);
        uuid[4] = input.substring(20, 32);
        return String.join("-", uuid);
    }
}
