package listeners.commands;

import com.github.twitch4j.chat.events.channel.ChannelMessageEvent;
import com.github.twitch4j.helix.domain.Stream;
import com.netflix.hystrix.exception.HystrixRuntimeException;
import util.TwitchApi;
import util.TwitchUserLevel.USER_LEVEL;

import static api.SpeedrunApi.*;

public class WrListener extends CommandBase {
    private static final CommandType COMMAND_TYPE = CommandType.PREFIX_COMMAND;
    private static final USER_LEVEL MIN_USER_LEVEL = USER_LEVEL.DEFAULT;
    private static final int COOLDOWN = 5;
    private static final CooldownType COOLDOWN_TYPE = CooldownType.COMBINED;
    private static final String PATTERN = "!wr";
    
    private static final String GAME_ID_BUG_FABLES = "511735";
    private static final String GAME_ID_SUNSHINE = "6086";
    private static final String GAME_ID_PAPER_MARIO = "18231";
    private static final String GAME_ID_TTYD = "6855";
    private static final String GAME_ID_OOT = "11557";
    private static final String GAME_ID_SMRPG_SWITCH = "1675405846";

    private final TwitchApi twitchApi;

    public WrListener(TwitchApi twitchApi) {
        super(COMMAND_TYPE, MIN_USER_LEVEL, COOLDOWN, COOLDOWN_TYPE, PATTERN);
        this.twitchApi = twitchApi;
    }

    @Override
    protected void performCommand(String command, USER_LEVEL userLevel, ChannelMessageEvent messageEvent) {
        Stream stream;
        try {
            stream = twitchApi.getStreamByUsername(twitchApi.getStreamerUser().getLogin());
        } catch (HystrixRuntimeException e) {
            e.printStackTrace();
            twitchApi.channelMessage("Error retrieving stream data");
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
                    wrText = getWr(Game.BUG_FABLES, BugFablesCategory.HUNDO);
                } else if (streamTitle.contains("glitchless")) {
                    wrText = getWr(Game.BUG_FABLES, BugFablesCategory.GLITCHLESS);
                } else if (streamTitle.contains("bosses")) {
                    wrText = getWr(Game.BUG_FABLES, BugFablesCategory.ALL_BOSSES);
                } else if (streamTitle.contains("chapters")) {
                    wrText = getWr(Game.BUG_FABLES, BugFablesCategory.ALL_CHAPTERS);
                } else if (streamTitle.contains("mystery")) {
                    wrText = getWr(Game.BUG_FABLES, BugFablesCategory.ANY_MYSTERY);
                } else if (streamTitle.contains("codes")) {
                    wrText = getWr(Game.BUG_FABLES, BugFablesCategory.ANY_ALL_CODES);
                } else if (streamTitle.contains("dll")) {
                    wrText = getWr(Game.BUG_FABLES, BugFablesCategory.ANY_DLL);
                } else {
                    wrText = getWr(Game.BUG_FABLES, BugFablesCategory.ANY_PERCENT);
                }
                break;
            case GAME_ID_SUNSHINE:
                if (streamTitle.contains("any%")) {
                    wrText = getWr(Game.SUNSHINE, SunshineCategory.ANY_PERCENT);
                } else if (streamTitle.contains("all episodes")) {
                    wrText = getWr(Game.SUNSHINE, SunshineCategory.ALL_EPISODES);
                } else if (streamTitle.contains("79")) {
                    wrText = getWr(Game.SUNSHINE, SunshineCategory.SHINES_79);
                } else if (streamTitle.contains("96")) {
                    wrText = getWr(Game.SUNSHINE, SunshineCategory.SHINES_96);
                } else if (streamTitle.contains("120")) {
                    wrText = getWr(Game.SUNSHINE, SunshineCategory.SHINES_120);
                }
                break;
            case GAME_ID_PAPER_MARIO:
                if (streamTitle.contains("any% (no peach warp)") || streamTitle.contains("any% (no pw)")) {
                    wrText = getWr(Game.PAPER_MARIO, PapeCategory.ANY_PERCENT_NO_PW, PapeVariable.N64);
                } else if (streamTitle.contains("any% no rng") || streamTitle.contains("any% (no rng)")) {
                    wrText = getWr(Game.PAPER_MARIO_MEMES, PapeCategory.ANY_NO_RNG, PapeVariable.N64);
                } else if (streamTitle.contains("any%")) {
                    wrText = getWr(Game.PAPER_MARIO, PapeCategory.ANY_PERCENT, PapeVariable.N64);
                } else if (streamTitle.contains("all cards") && !streamTitle.contains("reverse")) {
                    wrText = getWr(Game.PAPER_MARIO, PapeCategory.ALL_CARDS, PapeVariable.N64);
                } else if (streamTitle.contains("all bosses")) {
                    wrText = getWr(Game.PAPER_MARIO, PapeCategory.ALL_BOSSES, PapeVariable.N64);
                } else if (streamTitle.contains("glitchless")) {
                    wrText = getWr(Game.PAPER_MARIO, PapeCategory.GLITCHLESS, PapeVariable.N64);
                } else if (streamTitle.contains("100%")) {
                    wrText = getWr(Game.PAPER_MARIO, PapeCategory.HUNDO, PapeVariable.N64);
                } else if (streamTitle.contains("reverse") && streamTitle.contains("all cards")) {
                    wrText = getWr(Game.PAPER_MARIO, PapeCategory.REVERSE_ALL_CARDS, PapeVariable.N64);
                } else if (streamTitle.contains("pig") || streamTitle.contains("\uD83D\uDC37") || streamTitle.contains(
                        "oink")) {
                    wrText = getWr(Game.PAPER_MARIO_MEMES, PapeCategory.PIGGIES);
                } else if (streamTitle.contains("all bloops")) {
                    wrText = getWr(Game.PAPER_MARIO_MEMES, PapeCategory.ALL_BLOOPS);
                } else if (streamTitle.contains("chapter 1")) {
                    wrText = getWr(Game.PAPER_MARIO_MEMES, PapeCategory.BEAT_CHAPTER_1);
                } else if (streamTitle.contains("soapcake") || streamTitle.contains("soap cake")) {
                    wrText = getWr(Game.PAPER_MARIO_MEMES, PapeCategory.SOAP_CAKE);
                } else if (streamTitle.contains("mailman") || streamTitle.contains("amazon prime")) {
                    wrText = getWr(Game.PAPER_MARIO_MEMES, PapeCategory.MAILMAN);
                } else if (streamTitle.contains("no major sequence breaks") || streamTitle.contains("nmsb")) {
                    wrText = getWr(Game.PAPER_MARIO_MEMES, PapeCategory.NMSB);
                } else if (streamTitle.contains("swop")) {
                    wrText = getWr(Game.PAPER_MARIO_MEMES, PapeCategory.STOP_N_SWOP);
                }
                break;
            case GAME_ID_TTYD:
                if (streamTitle.contains("any%")) {
                    wrText = getWr(Game.TTYD, TtydCategory.ANY_PERCENT);
                } else if (streamTitle.contains("crystal stars")) {
                    wrText = getWr(Game.TTYD, TtydCategory.ALL_CRYSTAL_STARS);
                } else if (streamTitle.contains("100%") || streamTitle.contains("hundo")) {
                    wrText = getWr(Game.TTYD, TtydCategory.HUNDO);
                } else if (streamTitle.contains("glitchless")) {
                    wrText = getWr(Game.TTYD, TtydCategory.GLITCHLESS);
                } else if (streamTitle.contains("collectibles")) {
                    wrText = getWr(Game.TTYD, TtydCategory.ALL_COLLECTIBLES);
                } else if (streamTitle.contains("upgrades")) {
                    wrText = getWr(Game.TTYD, TtydCategory.MAX_UPGRADES);
                }
                break;
            case GAME_ID_OOT:
                if (streamTitle.contains("swop")) {
                    wrText = getWr(Game.PAPER_MARIO_MEMES, PapeCategory.STOP_N_SWOP);
                } else {
                    wrText = getWr(Game.OOT, OotCategory.ANY_PERCENT);
                }
                break;
            case GAME_ID_SMRPG_SWITCH:
                wrText = getWr(Game.SMRPG, SmrpgCategory.NORMAL_RTA, SmrpgVariable.TURBO);
                break;
        }
    
        twitchApi.channelMessage(String.format("@%s %s", messageEvent.getUser().getName(), wrText));
    }
}