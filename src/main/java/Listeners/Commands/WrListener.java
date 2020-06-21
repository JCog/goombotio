package Listeners.Commands;

import Util.TwirkInterface;
import Util.TwitchApi;
import Util.TwitchUserLevel;
import com.gikk.twirk.types.twitchMessage.TwitchMessage;
import com.gikk.twirk.types.users.TwitchUser;
import com.github.twitch4j.helix.domain.Stream;

import static APIs.SpeedrunApi.*;

public class WrListener extends CommandBase {
    
    private static final String GAME_SUNSHINE = "Super Mario Sunshine";
    private static final String GAME_PAPER_MARIO = "Paper Mario";
    private static final String GAME_TTYD = "Paper Mario: The Thousand-Year Door";
    private static final String PATTERN = "!wr";
    
    private final TwirkInterface twirk;
    private final TwitchApi twitchApi;

    public WrListener(TwirkInterface twirk, TwitchApi twitchApi) {
        super(CommandType.PREFIX_COMMAND);
        this.twirk = twirk;
        this.twitchApi = twitchApi;
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
        return 5000;
    }

    @Override
    protected void performCommand(String command, TwitchUser sender, TwitchMessage message) {
        Stream stream = twitchApi.getStream();
        String title = stream.getTitle().toLowerCase();
        String game = twitchApi.getGameById(stream.getGameId()).getName();
        String wrText = "Unknown WR";
        switch (game) {
            case GAME_SUNSHINE:
                if (title.contains("any%")) {
                    wrText = getWr(Game.SUNSHINE, SunshineCategory.ANY_PERCENT);
                } else if (title.contains("all episodes")) {
                    wrText = getWr(Game.SUNSHINE, SunshineCategory.ALL_EPISODES);
                } else if (title.contains("79")) {
                    wrText = getWr(Game.SUNSHINE, SunshineCategory.SHINES_79);
                } else if (title.contains("96")) {
                    wrText = getWr(Game.SUNSHINE, SunshineCategory.SHINES_96);
                } else if (title.contains("120")) {
                    wrText = getWr(Game.SUNSHINE, SunshineCategory.SHINES_120);
                }
                break;
            case GAME_PAPER_MARIO:
                if (title.contains("any% (no peach warp)") || title.contains("any% (no pw)")) {
                    wrText = getWr(Game.PAPER_MARIO, PapeCategory.ANY_PERCENT_NO_PW);
                } else if (title.contains("any% no rng") || title.contains("any% (no rng)")) {
                    wrText = getWr(Game.PAPER_MARIO_MEMES, PapeCategory.ANY_NO_RNG);
                } else if (title.contains("any%")) {
                    wrText = getWr(Game.PAPER_MARIO, PapeCategory.ANY_PERCENT);
                } else if (title.contains("all cards") && !title.contains("reverse")) {
                    wrText = getWr(Game.PAPER_MARIO, PapeCategory.ALL_CARDS);
                } else if (title.contains("all bosses")) {
                    wrText = getWr(Game.PAPER_MARIO, PapeCategory.ALL_BOSSES);
                } else if (title.contains("glitchless")) {
                    wrText = getWr(Game.PAPER_MARIO, PapeCategory.GLITCHLESS);
                } else if (title.contains("100%")) {
                    wrText = getWr(Game.PAPER_MARIO, PapeCategory.HUNDO);
                } else if (title.contains("pig") || title.contains("\uD83D\uDC37") || title.contains("oink")) {
                    wrText = getWr(Game.PAPER_MARIO_MEMES, PapeCategory.PIGGIES);
                } else if (title.contains("all bloops")) {
                    wrText = getWr(Game.PAPER_MARIO_MEMES, PapeCategory.ALL_BLOOPS);
                } else if (title.contains("chapter 1")) {
                    wrText = getWr(Game.PAPER_MARIO_MEMES, PapeCategory.BEAT_CHAPTER_1);
                } else if (title.contains("soapcake") || title.contains("soap cake")) {
                    wrText = getWr(Game.PAPER_MARIO_MEMES, PapeCategory.SOAP_CAKE);
                } else if (title.contains("reverse") && title.contains("all cards")) {
                    wrText = getWr(Game.PAPER_MARIO_MEMES, PapeCategory.REVERSE_ALL_CARDS);
                } else if (title.contains("mailman") || title.contains("amazon prime")) {
                    wrText = getWr(Game.PAPER_MARIO_MEMES, PapeCategory.MAILMAN);
                }
                break;
            case GAME_TTYD:
                if (title.contains("any%") && (title.contains("japanese") || title.contains("jp"))) {
                    wrText = getWr(Game.TTYD, TtydCategory.ANY_PERCENT_JP);
                } else if (title.contains("crystal stars")) {
                    wrText = getWr(Game.TTYD, TtydCategory.ALL_CRYSTAL_STARS);
                } else if (title.contains("100%") || title.contains("hundo")) {
                    wrText = getWr(Game.TTYD, TtydCategory.HUNDO);
                } else if (title.contains("glitchless")) {
                    wrText = getWr(Game.TTYD, TtydCategory.GLITCHLESS);
                } else if (title.contains("collectibles")) {
                    wrText = getWr(Game.TTYD, TtydCategory.ALL_COLLECTIBLES);
                } else if (title.contains("upgrades")) {
                    wrText = getWr(Game.TTYD, TtydCategory.MAX_UPGRADES);
                }
                break;
        }
        
        twirk.channelMessage(String.format("@%s %s", sender.getDisplayName(), wrText));
    }
}