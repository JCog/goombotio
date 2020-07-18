package Listeners.Commands;

import APIs.MinecraftApi;
import Database.Misc.MinecraftUserDb;
import Util.TwirkInterface;
import Util.TwitchUserLevel;
import com.gikk.twirk.types.twitchMessage.TwitchMessage;
import com.gikk.twirk.types.users.TwitchUser;

import java.util.ArrayList;

public class MinecraftListener extends CommandBase {
    private static final String PATTERN = "!minecraft";
    private static final String GENERIC_MESSAGE = "We have a Minecraft server for subs! To join, register your Minecraft username by typing \"!minecraft <username>\" in the chat and you'll automatically be added to the whitelist if you're subbed to the channel. Then, add a new server with the address \"minecraft.jcoggers.com\". Have fun! PunchTrees";

    private final MinecraftUserDb minecraftUserDb = MinecraftUserDb.getInstance();
    private final TwirkInterface twirk;

    public MinecraftListener(TwirkInterface twirk) {
        super(CommandType.PREFIX_COMMAND);
        this.twirk = twirk;
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
            twirk.channelMessage(GENERIC_MESSAGE);
            return;
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

        String uuid = profile.get(0);
        String name = profile.get(1);
        minecraftUserDb.addUser(Long.toString(sender.getUserID()), uuid, name);
        twirk.channelMessage(String.format(
                "@%s Minecraft username updated to \"%s\"",
                sender.getDisplayName(),
                name
        ));
    }
}
