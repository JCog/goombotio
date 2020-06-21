package Listeners.Commands;

import APIs.SpeedrunApi;
import Util.TwirkInterface;
import Util.TwitchApi;
import Util.TwitchUserLevel;
import com.gikk.twirk.types.twitchMessage.TwitchMessage;
import com.gikk.twirk.types.users.TwitchUser;
import com.github.twitch4j.helix.domain.Game;
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
        String streamTitle = "";
        String gameTitle = "";
        if (stream != null) {
            streamTitle = stream.getTitle().toLowerCase();
            Game game = twitchApi.getGameById(stream.getGameId());
            if (game != null) {
                gameTitle = game.getName();
            }
        }
        String wrText = "Unknown WR";
        switch (gameTitle) {
            case GAME_SUNSHINE:
                if (streamTitle.contains("any%")) {
                    wrText = getWr(SpeedrunApi.Game.SUNSHINE, SunshineCategory.ANY_PERCENT);
                } else if (streamTitle.contains("all episodes")) {
                    wrText = getWr(SpeedrunApi.Game.SUNSHINE, SunshineCategory.ALL_EPISODES);
                } else if (streamTitle.contains("79")) {
                    wrText = getWr(SpeedrunApi.Game.SUNSHINE, SunshineCategory.SHINES_79);
                } else if (streamTitle.contains("96")) {
                    wrText = getWr(SpeedrunApi.Game.SUNSHINE, SunshineCategory.SHINES_96);
                } else if (streamTitle.contains("120")) {
                    wrText = getWr(SpeedrunApi.Game.SUNSHINE, SunshineCategory.SHINES_120);
                }
                break;
            case GAME_PAPER_MARIO:
                if (streamTitle.contains("any% (no peach warp)") || streamTitle.contains("any% (no pw)")) {
                    wrText = getWr(SpeedrunApi.Game.PAPER_MARIO, PapeCategory.ANY_PERCENT_NO_PW);
                } else if (streamTitle.contains("any% no rng") || streamTitle.contains("any% (no rng)")) {
                    wrText = getWr(SpeedrunApi.Game.PAPER_MARIO_MEMES, PapeCategory.ANY_NO_RNG);
                } else if (streamTitle.contains("any%")) {
                    wrText = getWr(SpeedrunApi.Game.PAPER_MARIO, PapeCategory.ANY_PERCENT);
                } else if (streamTitle.contains("all cards") && !streamTitle.contains("reverse")) {
                    wrText = getWr(SpeedrunApi.Game.PAPER_MARIO, PapeCategory.ALL_CARDS);
                } else if (streamTitle.contains("all bosses")) {
                    wrText = getWr(SpeedrunApi.Game.PAPER_MARIO, PapeCategory.ALL_BOSSES);
                } else if (streamTitle.contains("glitchless")) {
                    wrText = getWr(SpeedrunApi.Game.PAPER_MARIO, PapeCategory.GLITCHLESS);
                } else if (streamTitle.contains("100%")) {
                    wrText = getWr(SpeedrunApi.Game.PAPER_MARIO, PapeCategory.HUNDO);
                } else if (streamTitle.contains("pig") || streamTitle.contains("\uD83D\uDC37") || streamTitle.contains("oink")) {
                    wrText = getWr(SpeedrunApi.Game.PAPER_MARIO_MEMES, PapeCategory.PIGGIES);
                } else if (streamTitle.contains("all bloops")) {
                    wrText = getWr(SpeedrunApi.Game.PAPER_MARIO_MEMES, PapeCategory.ALL_BLOOPS);
                } else if (streamTitle.contains("chapter 1")) {
                    wrText = getWr(SpeedrunApi.Game.PAPER_MARIO_MEMES, PapeCategory.BEAT_CHAPTER_1);
                } else if (streamTitle.contains("soapcake") || streamTitle.contains("soap cake")) {
                    wrText = getWr(SpeedrunApi.Game.PAPER_MARIO_MEMES, PapeCategory.SOAP_CAKE);
                } else if (streamTitle.contains("reverse") && streamTitle.contains("all cards")) {
                    wrText = getWr(SpeedrunApi.Game.PAPER_MARIO_MEMES, PapeCategory.REVERSE_ALL_CARDS);
                } else if (streamTitle.contains("mailman") || streamTitle.contains("amazon prime")) {
                    wrText = getWr(SpeedrunApi.Game.PAPER_MARIO_MEMES, PapeCategory.MAILMAN);
                }
                break;
            case GAME_TTYD:
                if (streamTitle.contains("any%") && (streamTitle.contains("japanese") || streamTitle.contains("jp"))) {
                    wrText = getWr(SpeedrunApi.Game.TTYD, TtydCategory.ANY_PERCENT_JP);
                } else if (streamTitle.contains("crystal stars")) {
                    wrText = getWr(SpeedrunApi.Game.TTYD, TtydCategory.ALL_CRYSTAL_STARS);
                } else if (streamTitle.contains("100%") || streamTitle.contains("hundo")) {
                    wrText = getWr(SpeedrunApi.Game.TTYD, TtydCategory.HUNDO);
                } else if (streamTitle.contains("glitchless")) {
                    wrText = getWr(SpeedrunApi.Game.TTYD, TtydCategory.GLITCHLESS);
                } else if (streamTitle.contains("collectibles")) {
                    wrText = getWr(SpeedrunApi.Game.TTYD, TtydCategory.ALL_COLLECTIBLES);
                } else if (streamTitle.contains("upgrades")) {
                    wrText = getWr(SpeedrunApi.Game.TTYD, TtydCategory.MAX_UPGRADES);
                }
                break;
        }
        
        twirk.channelMessage(String.format("@%s %s", sender.getDisplayName(), wrText));
    }
}