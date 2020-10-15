package listeners.events;

import com.gikk.twirk.events.TwirkListener;
import com.gikk.twirk.types.twitchMessage.TwitchMessage;
import com.gikk.twirk.types.users.TwitchUser;
import com.jcog.utils.database.DbManager;
import com.jcog.utils.database.misc.BitWarDb;
import util.TwirkInterface;

public class BitListener implements TwirkListener {
    private static final String BIT_WAR_NAME = "save_kill_yoshi";
    private static final String TEAM_SAVE = "team_save";
    private static final String TEAM_KILL = "team_kill";
    private static final String SAVE_KEYWORD = "save";
    private static final String KILL_KEYWORD = "kill";

    private final TwirkInterface twirk;
    private final BitWarDb bitWarDb;

    public BitListener(TwirkInterface twirk, DbManager dbManager) {
        this.twirk = twirk;
        bitWarDb = dbManager.getBitWarDb();
    }

    @Override
    public void onPrivMsg(TwitchUser sender, TwitchMessage message) {
        if (message.isCheer()) {
            String messageText = message.getContent().toLowerCase();
            if (messageText.contains(SAVE_KEYWORD)) {
                bitWarDb.addBits(BIT_WAR_NAME, TEAM_SAVE, message.getBits());
                twirk.channelMessage(String.format(
                        "%d bits have been put toward saving Yoshi jcogLove",
                        message.getBits()
                ));
            }
            else if (messageText.contains(KILL_KEYWORD)) {
                bitWarDb.addBits(BIT_WAR_NAME, TEAM_KILL, message.getBits());
                twirk.channelMessage(String.format(
                        "%d bits have been put toward killing Yoshi jcogBan",
                        message.getBits()
                ));
            }
        }
    }
}
