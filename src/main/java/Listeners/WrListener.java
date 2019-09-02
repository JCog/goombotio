package Listeners;

import APIs.SpeedrunApi;
import Functions.StreamInfo;
import com.gikk.twirk.Twirk;
import com.gikk.twirk.enums.USER_TYPE;
import com.gikk.twirk.types.twitchMessage.TwitchMessage;
import com.gikk.twirk.types.users.TwitchUser;

import static APIs.SpeedrunApi.*;

public class WrListener extends CommandBase {

    private final static String pattern = "!wr";
    private final Twirk twirk;
    private final StreamInfo streamInfo;
    private final String GAME_PAPER_MARIO = "Paper Mario";

    public WrListener(Twirk twirk, StreamInfo streamInfo) {
        super(CommandType.PREFIX_COMMAND);
        this.twirk = twirk;
        this.streamInfo = streamInfo;
    }

    @Override
    protected String getCommandWords() {
        return pattern;
    }

    @Override
    protected USER_TYPE getMinUserPrivilege() {
        return USER_TYPE.DEFAULT;
    }

    @Override
    protected int getCooldownLength() {
        return 5000;
    }

    @Override
    protected void performCommand(String command, TwitchUser sender, TwitchMessage message) {
        String title = streamInfo.getTitle().toLowerCase();
        String game = streamInfo.getGame();
        if (game.equals(GAME_PAPER_MARIO)) {
            if (title.contains("any%")) {
                twirk.channelMessage(String.format("@%s %s", sender.getDisplayName(), getPapeWr(Game.PAPER_MARIO, PapeCategory.ANY_PERCENT)));
            }
            else if (title.contains("any% (no peach warp)")) {
                twirk.channelMessage(String.format("@%s %s", sender.getDisplayName(), getPapeWr(Game.PAPER_MARIO, PapeCategory.ANY_PERCENT_NO_PW)));
            }
            else if (title.contains("all cards")) {
                twirk.channelMessage(String.format("@%s %s", sender.getDisplayName(), getPapeWr(Game.PAPER_MARIO, PapeCategory.ALL_CARDS)));
            }
            else if (title.contains("all bosses")) {
                twirk.channelMessage(String.format("@%s %s", sender.getDisplayName(), getPapeWr(Game.PAPER_MARIO, PapeCategory.ALL_BOSSES)));
            }
            else if (title.contains("glitchless")) {
                twirk.channelMessage(String.format("@%s %s", sender.getDisplayName(), getPapeWr(Game.PAPER_MARIO, PapeCategory.GLITCHLESS)));
            }
            else if (title.contains("100%")) {
                twirk.channelMessage(String.format("@%s %s", sender.getDisplayName(), getPapeWr(Game.PAPER_MARIO, PapeCategory.HUNDO)));
            }
        }
    }
}