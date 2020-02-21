package Listeners.Commands;

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
    private final String GAME_TTYD = "Paper Mario: The Thousand-Year Door";

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
            if (title.contains("any% (no peach warp)") || title.contains("any% (no pw)")) {
                twirk.channelMessage(String.format("@%s %s", sender.getDisplayName(), getWr(Game.PAPER_MARIO, PapeCategory.ANY_PERCENT_NO_PW)));
            }
            else if (title.contains("any% no rng") || title.contains("any% (no rng)")) {
                twirk.channelMessage(String.format("@%s %s", sender.getDisplayName(), getWr(Game.PAPER_MARIO_MEMES, PapeCategory.ANY_NO_RNG)));
            }
            else if (title.contains("any%")) {
                twirk.channelMessage(String.format("@%s %s", sender.getDisplayName(), getWr(Game.PAPER_MARIO, PapeCategory.ANY_PERCENT)));
            }
            else if (title.contains("all cards") && !title.contains("reverse")) {
                twirk.channelMessage(String.format("@%s %s", sender.getDisplayName(), getWr(Game.PAPER_MARIO, PapeCategory.ALL_CARDS)));
            }
            else if (title.contains("all bosses")) {
                twirk.channelMessage(String.format("@%s %s", sender.getDisplayName(), getWr(Game.PAPER_MARIO, PapeCategory.ALL_BOSSES)));
            }
            else if (title.contains("glitchless")) {
                twirk.channelMessage(String.format("@%s %s", sender.getDisplayName(), getWr(Game.PAPER_MARIO, PapeCategory.GLITCHLESS)));
            }
            else if (title.contains("100%")) {
                twirk.channelMessage(String.format("@%s %s", sender.getDisplayName(), getWr(Game.PAPER_MARIO, PapeCategory.HUNDO)));
            }
            else if (title.contains("pig") || title.contains("\uD83D\uDC37") || title.contains("oink")) {
                twirk.channelMessage(String.format("@%s %s", sender.getDisplayName(), getWr(Game.PAPER_MARIO_MEMES, PapeCategory.PIGGIES)));
            }
            else if (title.contains("all bloops")) {
                twirk.channelMessage(String.format("@%s %s", sender.getDisplayName(), getWr(Game.PAPER_MARIO_MEMES, PapeCategory.ALL_BLOOPS)));
            }
            else if (title.contains("chapter 1")) {
                twirk.channelMessage(String.format("@%s %s", sender.getDisplayName(), getWr(Game.PAPER_MARIO_MEMES, PapeCategory.BEAT_CHAPTER_1)));
            }
            else if (title.contains("soapcake") || title.contains("soap cake")) {
                twirk.channelMessage(String.format("@%s %s", sender.getDisplayName(), getWr(Game.PAPER_MARIO_MEMES, PapeCategory.SOAP_CAKE)));
            }
            else if (title.contains("reverse") && title.contains("all cards")) {
                twirk.channelMessage(String.format("@%s %s", sender.getDisplayName(), getWr(Game.PAPER_MARIO_MEMES, PapeCategory.REVERSE_ALL_CARDS)));
            }
            else {
                twirk.channelMessage(String.format("@%s Unknown WR", sender.getDisplayName()));
            }
        }
        else if (game.equals(GAME_TTYD)) {
            if (title.contains("any%") && (title.contains("japanese") || title.contains("jp"))) {
                twirk.channelMessage(String.format("@%s %s", sender.getDisplayName(), getWr(Game.TTYD, TtydCategory.ANY_PERCENT_JP)));
            }
            else if (title.contains("crystal stars")) {
                twirk.channelMessage(String.format("@%s %s", sender.getDisplayName(), getWr(Game.TTYD, TtydCategory.ALL_CRYSTAL_STARS)));
            }
            else if (title.contains("100%") || title.contains("hundo")) {
                twirk.channelMessage(String.format("@%s %s", sender.getDisplayName(), getWr(Game.TTYD, TtydCategory.HUNDO)));
            }
            else if (title.contains("glitchless")) {
                twirk.channelMessage(String.format("@%s %s", sender.getDisplayName(), getWr(Game.TTYD, TtydCategory.GLITCHLESS)));
            }
            else if (title.contains("collectibles")) {
                twirk.channelMessage(String.format("@%s %s", sender.getDisplayName(), getWr(Game.TTYD, TtydCategory.ALL_COLLECTIBLES)));
            }
            else if (title.contains("upgrades")) {
                twirk.channelMessage(String.format("@%s %s", sender.getDisplayName(), getWr(Game.TTYD, TtydCategory.MAX_UPGRADES)));
            }
            else {
                twirk.channelMessage(String.format("@%s Unknown WR", sender.getDisplayName()));
            }
        }
    }
}