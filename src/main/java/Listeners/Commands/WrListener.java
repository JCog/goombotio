package Listeners.Commands;

import APIs.SpeedrunApi;
import Util.TwirkInterface;
import com.gikk.twirk.types.twitchMessage.TwitchMessage;
import com.gikk.twirk.types.users.TwitchUser;
import com.github.twitch4j.helix.domain.Stream;
import com.jcog.utils.TwitchApi;
import com.jcog.utils.TwitchUserLevel;
import com.netflix.hystrix.exception.HystrixRuntimeException;

import java.util.concurrent.ScheduledExecutorService;

import static APIs.SpeedrunApi.*;

public class WrListener extends CommandBase {

    private static final String GAME_ID_BUG_FABLES = "511735";
    private static final String GAME_ID_SUNSHINE = "6086";
    private static final String GAME_ID_PAPER_MARIO = "18231";
    private static final String GAME_ID_TTYD = "6855";
    private static final String PATTERN = "!wr";

    private final TwirkInterface twirk;
    private final TwitchApi twitchApi;

    public WrListener(ScheduledExecutorService scheduler, TwirkInterface twirk, TwitchApi twitchApi) {
        super(CommandType.PREFIX_COMMAND, scheduler);
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
        Stream stream;
        try {
            stream = twitchApi.getStream();
        }
        catch (HystrixRuntimeException e) {
            e.printStackTrace();
            twirk.channelMessage("Error retrieving stream data");
            return;
        }
        String streamTitle = "";
        String gameId = "";
        if (stream != null) {
            streamTitle = stream.getTitle().toLowerCase();
            gameId = stream.getGameId();
        }
        String wrText = "Unknown WR";
        switch (gameId) {
            case GAME_ID_BUG_FABLES:
                if (streamTitle.contains("100%") || streamTitle.contains("hundo") || streamTitle.contains("\uD83D\uDCAF")) {
                    wrText = getWr(SpeedrunApi.Game.BUG_FABLES, BugFablesCategory.HUNDO);
                }
                else if (streamTitle.contains("glitchless")) {
                    wrText = getWr(SpeedrunApi.Game.BUG_FABLES, BugFablesCategory.GLITCHLESS);
                }
                else if (streamTitle.contains("bosses")) {
                    wrText = getWr(SpeedrunApi.Game.BUG_FABLES, BugFablesCategory.ALL_BOSSES);
                }
                else if (streamTitle.contains("chapters")) {
                    wrText = getWr(SpeedrunApi.Game.BUG_FABLES, BugFablesCategory.ALL_CHAPTERS);
                }
                else if (streamTitle.contains("mystery")) {
                    wrText = getWr(SpeedrunApi.Game.BUG_FABLES, BugFablesCategory.ANY_MYSTERY);
                }
                else if (streamTitle.contains("codes")) {
                    wrText = getWr(SpeedrunApi.Game.BUG_FABLES, BugFablesCategory.ANY_ALL_CODES);
                }
                else if (streamTitle.contains("dll")) {
                    wrText = getWr(SpeedrunApi.Game.BUG_FABLES, BugFablesCategory.ANY_DLL);
                }
                else {
                    wrText = getWr(SpeedrunApi.Game.BUG_FABLES, BugFablesCategory.ANY_PERCENT);
                }
                break;
            case GAME_ID_SUNSHINE:
                if (streamTitle.contains("any%")) {
                    wrText = getWr(SpeedrunApi.Game.SUNSHINE, SunshineCategory.ANY_PERCENT);
                }
                else if (streamTitle.contains("all episodes")) {
                    wrText = getWr(SpeedrunApi.Game.SUNSHINE, SunshineCategory.ALL_EPISODES);
                }
                else if (streamTitle.contains("79")) {
                    wrText = getWr(SpeedrunApi.Game.SUNSHINE, SunshineCategory.SHINES_79);
                }
                else if (streamTitle.contains("96")) {
                    wrText = getWr(SpeedrunApi.Game.SUNSHINE, SunshineCategory.SHINES_96);
                }
                else if (streamTitle.contains("120")) {
                    wrText = getWr(SpeedrunApi.Game.SUNSHINE, SunshineCategory.SHINES_120);
                }
                break;
            case GAME_ID_PAPER_MARIO:
                if (streamTitle.contains("any% (no peach warp)") || streamTitle.contains("any% (no pw)")) {
                    wrText = getWr(SpeedrunApi.Game.PAPER_MARIO, PapeCategory.ANY_PERCENT_NO_PW);
                }
                else if (streamTitle.contains("any% no rng") || streamTitle.contains("any% (no rng)")) {
                    wrText = getWr(SpeedrunApi.Game.PAPER_MARIO_MEMES, PapeCategory.ANY_NO_RNG);
                }
                else if (streamTitle.contains("any%")) {
                    wrText = getWr(SpeedrunApi.Game.PAPER_MARIO, PapeCategory.ANY_PERCENT);
                }
                else if (streamTitle.contains("all cards") && !streamTitle.contains("reverse")) {
                    wrText = getWr(SpeedrunApi.Game.PAPER_MARIO, PapeCategory.ALL_CARDS);
                }
                else if (streamTitle.contains("all bosses")) {
                    wrText = getWr(SpeedrunApi.Game.PAPER_MARIO, PapeCategory.ALL_BOSSES);
                }
                else if (streamTitle.contains("glitchless")) {
                    wrText = getWr(SpeedrunApi.Game.PAPER_MARIO, PapeCategory.GLITCHLESS);
                }
                else if (streamTitle.contains("100%")) {
                    wrText = getWr(SpeedrunApi.Game.PAPER_MARIO, PapeCategory.HUNDO);
                }
                else if (streamTitle.contains("pig") || streamTitle.contains("\uD83D\uDC37") || streamTitle.contains("oink")) {
                    wrText = getWr(SpeedrunApi.Game.PAPER_MARIO_MEMES, PapeCategory.PIGGIES);
                }
                else if (streamTitle.contains("all bloops")) {
                    wrText = getWr(SpeedrunApi.Game.PAPER_MARIO_MEMES, PapeCategory.ALL_BLOOPS);
                }
                else if (streamTitle.contains("chapter 1")) {
                    wrText = getWr(SpeedrunApi.Game.PAPER_MARIO_MEMES, PapeCategory.BEAT_CHAPTER_1);
                }
                else if (streamTitle.contains("soapcake") || streamTitle.contains("soap cake")) {
                    wrText = getWr(SpeedrunApi.Game.PAPER_MARIO_MEMES, PapeCategory.SOAP_CAKE);
                }
                else if (streamTitle.contains("reverse") && streamTitle.contains("all cards")) {
                    wrText = getWr(SpeedrunApi.Game.PAPER_MARIO_MEMES, PapeCategory.REVERSE_ALL_CARDS);
                }
                else if (streamTitle.contains("mailman") || streamTitle.contains("amazon prime")) {
                    wrText = getWr(SpeedrunApi.Game.PAPER_MARIO_MEMES, PapeCategory.MAILMAN);
                }
                break;
            case GAME_ID_TTYD:
                if (streamTitle.contains("any%") && (streamTitle.contains("japanese") || streamTitle.contains("jp"))) {
                    wrText = getWr(SpeedrunApi.Game.TTYD, TtydCategory.ANY_PERCENT_JP);
                }
                else if (streamTitle.contains("crystal stars")) {
                    wrText = getWr(SpeedrunApi.Game.TTYD, TtydCategory.ALL_CRYSTAL_STARS);
                }
                else if (streamTitle.contains("100%") || streamTitle.contains("hundo")) {
                    wrText = getWr(SpeedrunApi.Game.TTYD, TtydCategory.HUNDO);
                }
                else if (streamTitle.contains("glitchless")) {
                    wrText = getWr(SpeedrunApi.Game.TTYD, TtydCategory.GLITCHLESS);
                }
                else if (streamTitle.contains("collectibles")) {
                    wrText = getWr(SpeedrunApi.Game.TTYD, TtydCategory.ALL_COLLECTIBLES);
                }
                else if (streamTitle.contains("upgrades")) {
                    wrText = getWr(SpeedrunApi.Game.TTYD, TtydCategory.MAX_UPGRADES);
                }
                break;
        }

        twirk.channelMessage(String.format("@%s %s", sender.getDisplayName(), wrText));
    }
}