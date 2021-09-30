package listeners.commands;

import api.MinecraftApi;
import com.gikk.twirk.types.twitchMessage.TwitchMessage;
import com.gikk.twirk.types.users.TwitchUser;
import database.DbManager;
import database.misc.MinecraftUserDb;
import functions.MinecraftWhitelistUpdater;
import util.TwirkInterface;
import util.TwitchUserLevel;

import java.util.ArrayList;
import java.util.concurrent.ScheduledExecutorService;

public class MinecraftListener extends CommandBase {
    private static final String PATTERN = "!minecraft";
    private static final String GENERIC_MESSAGE = "We have a community Minecraft server! It's currently open to %s. To join, register your Minecraft username by typing \"!minecraft <username>\" in the chat and you'll automatically be added to the whitelist. Then, add a new server with the address \"minecraft.jcoggers.com\". Have fun! PunchTrees";
    private static final String SUBS_ENABLED = "Sub-only mode has been enabled for the minecraft server.";
    private static final String SUBS_DISABLED = "Sub-only mode has been disabled for the minecraft server.";

    private final MinecraftUserDb minecraftUserDb;
    private final TwirkInterface twirk;
    private final MinecraftWhitelistUpdater mcUpdater;

    public MinecraftListener(ScheduledExecutorService scheduler, TwirkInterface twirk, DbManager dbManager, MinecraftWhitelistUpdater mcUpdater) {
        super(CommandType.PREFIX_COMMAND, scheduler);
        this.twirk = twirk;
        this.mcUpdater = mcUpdater;
        minecraftUserDb = dbManager.getMinecraftUserDb();
    }

    @Override
    public String getCommandWords() {
        return PATTERN;
    }

    @Override
    protected TwitchUserLevel.USER_LEVEL getMinUserPrivilege() {
        return TwitchUserLevel.USER_LEVEL.DEFAULT;
    }

    @Override
    protected int getCooldownLength() {
        return 0;
    }

    @Override
    protected void performCommand(String command, TwitchUser sender, TwitchMessage message) {
        String[] messageSplit = message.getContent().trim().split("\\s");
        if (messageSplit.length == 1) {
            String subs;
            if (mcUpdater.isSubOnly()) {
                subs = "subs only";
            }
            else {
                subs = "everyone";
            }
            twirk.channelMessage(String.format(GENERIC_MESSAGE, subs));
            return;
        }
    
        if (sender.isOwner()) {
            if (messageSplit[1].equals("1")) {
                mcUpdater.setSubOnly(true);
                twirk.channelMessage(SUBS_ENABLED);
                return;
            }
            else if (messageSplit[1].equals("0")) {
                mcUpdater.setSubOnly(false);
                twirk.channelMessage(SUBS_DISABLED);
                return;
            }
        }

        String mcUsername = messageSplit[1];
        ArrayList<String> profile = MinecraftApi.getProfile(mcUsername);
        if (profile == null) {
            twirk.channelMessage(String.format(
                    "@%s invalid Minecraft username \"%s\"",
                    sender.getDisplayName(),
                    mcUsername
            ));
            return;
        }

        String uuid = convertStringToUuid(profile.get(0));
        String name = profile.get(1);
        minecraftUserDb.addUser(Long.toString(sender.getUserID()), uuid, name);
        twirk.channelMessage(String.format(
                "@%s Minecraft username updated to \"%s\"",
                sender.getDisplayName(),
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
