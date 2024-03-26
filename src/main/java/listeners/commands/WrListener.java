package listeners.commands;

import api.src.SrcApi;
import api.src.SrcEnums;
import com.github.twitch4j.chat.events.channel.ChannelMessageEvent;
import com.github.twitch4j.helix.domain.Stream;
import com.netflix.hystrix.exception.HystrixRuntimeException;
import util.CommonUtils;
import util.TwitchApi;
import util.TwitchUserLevel.USER_LEVEL;

import static api.src.SrcEnums.BugFablesCategory.*;
import static api.src.SrcEnums.HmFomtCategory.HM_KAREN_GLITCHED;
import static api.src.SrcEnums.HmFomtCategory.HM_KAREN_GLITCHLESS;
import static api.src.SrcEnums.OotCategory.OOT_ANY_PERCENT;
import static api.src.SrcEnums.OotCategory.OOT_GLITCHLESS_AMQ;
import static api.src.SrcEnums.PapeCategory.*;
import static api.src.SrcEnums.PapeMemesCategory.*;
import static api.src.SrcEnums.SmrpgCategory.SMRPG_NORMAL_RTA_TURBO;
import static api.src.SrcEnums.SmsCategory.*;
import static api.src.SrcEnums.TtydCategory.*;

public class WrListener extends CommandBase {
    private static final CommandType COMMAND_TYPE = CommandType.PREFIX_COMMAND;
    private static final USER_LEVEL MIN_USER_LEVEL = USER_LEVEL.DEFAULT;
    private static final int COOLDOWN = 5;
    private static final CooldownType COOLDOWN_TYPE = CooldownType.COMBINED;
    private static final String PATTERN = "!wr";
    
    private static final String GAME_ID_BUG_FABLES = "511735";
    private static final String GAME_ID_HM_FOMT = "14599";
    private static final String GAME_ID_OOT = "11557";
    private static final String GAME_ID_PAPER_MARIO = "18231";
    private static final String GAME_ID_SMRPG_SWITCH = "1675405846";
    private static final String GAME_ID_SMS = "6086";
    private static final String GAME_ID_TTYD = "6855";

    private final TwitchApi twitchApi;
    private final SrcApi srcApi;

    public WrListener(CommonUtils commonUtils) {
        super(COMMAND_TYPE, MIN_USER_LEVEL, COOLDOWN, COOLDOWN_TYPE, PATTERN);
        twitchApi = commonUtils.getTwitchApi();
        srcApi = commonUtils.getApiManager().getSrcApi();
    }

    @Override
    protected void performCommand(String command, USER_LEVEL userLevel, ChannelMessageEvent messageEvent) {
        Stream stream;
        try {
            stream = twitchApi.getStreamByUserId(twitchApi.getStreamerUser().getId());
        } catch (HystrixRuntimeException e) {
            e.printStackTrace();
            twitchApi.channelMessage("Error retrieving stream data");
            return;
        }
        if (stream == null) {
            return;
        }
        
        String streamTitle = stream.getTitle().toLowerCase();
        String gameId = stream.getGameId();
        SrcEnums.Category category = null;
        switch (gameId) {
            case GAME_ID_BUG_FABLES:
                if (streamTitle.contains("100%") || streamTitle.contains("hundo") || streamTitle.contains("\uD83D\uDCAF")) {
                    category = BF_HUNDO;
                } else if (streamTitle.contains("glitchless")) {
                    category = BF_GLITCHLESS;
                } else if (streamTitle.contains("bosses")) {
                    category = BF_ALL_BOSSES;
                } else if (streamTitle.contains("chapters")) {
                    category = BF_ALL_CHAPTERS;
                } else if (streamTitle.contains("mystery")) {
                    category = BF_ANY_MYSTERY;
                } else if (streamTitle.contains("codes")) {
                    category = BF_ANY_ALL_CODES;
                } else if (streamTitle.contains("dll")) {
                    category = BF_ANY_DLL;
                } else {
                    category = BF_ANY_PERCENT;
                }
                break;
            case GAME_ID_SMS:
                if (streamTitle.contains("all episodes")) {
                    category = SMS_ALL_EPISODES;
                } else if (streamTitle.contains("79")) {
                    category = SMS_SHINES_79;
                } else if (streamTitle.contains("96")) {
                    category = SMS_SHINES_96;
                } else if (streamTitle.contains("120")) {
                    category = SMS_SHINES_120;
                } else {
                    category = SMS_ANY_PERCENT;
                }
                break;
            case GAME_ID_PAPER_MARIO:
                if (streamTitle.contains("any% (no peach warp)") || streamTitle.contains("any% (no pw)")) {
                    category = PAPE_ANY_PERCENT_NO_PW;
                } else if (streamTitle.contains("any%") &&(streamTitle.contains("no ace") || streamTitle.contains("acephobic"))) {
                    category = PAPE_ANY_PERCENT;
                } else if (streamTitle.contains("any% no rng") || streamTitle.contains("any% (no rng)")) {
                    category = PAPE_ANY_NO_RNG;
                } else if (streamTitle.contains("all cards") && !streamTitle.contains("reverse")) {
                    category = PAPE_ALL_CARDS;
                } else if (streamTitle.contains("all bosses")) {
                    category = PAPE_ALL_BOSSES;
                } else if (streamTitle.contains("glitchless")) {
                    category = PAPE_GLITCHLESS;
                } else if (streamTitle.contains("100%")) {
                    category = PAPE_HUNDO;
                } else if (streamTitle.contains("reverse") && streamTitle.contains("all cards")) {
                    category = PAPE_REVERSE_ALL_CARDS;
                } else if (streamTitle.contains("pig") || streamTitle.contains("\uD83D\uDC37") || streamTitle.contains("oink")) {
                    category = PAPE_PIGGIES;
                } else if (streamTitle.contains("all bloops")) {
                    category = PAPE_ALL_BLOOPS;
                } else if (streamTitle.contains("chapter 1")) {
                    category = PAPE_BEAT_CHAPTER_1;
                } else if (streamTitle.contains("soapcake") || streamTitle.contains("soap cake")) {
                    category = PAPE_SOAP_CAKE;
                } else if (streamTitle.contains("mailman") || streamTitle.contains("amazon prime")) {
                    category = PAPE_MAILMAN;
                } else if (streamTitle.contains("no major sequence breaks") || streamTitle.contains("nmsb")) {
                    category = PAPE_NMSB;
                } else if (streamTitle.contains("swop")) {
                    category = PAPE_STOP_N_SWOP;
                } else {
                    category = PAPE_ANY_PERCENT;
                }
                break;
            case GAME_ID_TTYD:
                if (streamTitle.contains("crystal stars")) {
                    category = TTYD_ALL_CRYSTAL_STARS;
                } else if (streamTitle.contains("100%") || streamTitle.contains("hundo")) {
                    category = TTYD_HUNDO;
                } else if (streamTitle.contains("glitchless")) {
                    category = TTYD_GLITCHLESS;
                } else if (streamTitle.contains("collectibles")) {
                    category = TTYD_ALL_COLLECTIBLES;
                } else if (streamTitle.contains("upgrades")) {
                    category = TTYD_MAX_UPGRADES;
                } else {
                    category = TTYD_ANY_PERCENT;
                }
                break;
            case GAME_ID_OOT:
                if (streamTitle.contains("swop")) {
                    category = PAPE_STOP_N_SWOP;
                } else if (streamTitle.contains("glitchless")){
                    category = OOT_GLITCHLESS_AMQ;
                } else {
                    category = OOT_ANY_PERCENT;
                }
                break;
            case GAME_ID_SMRPG_SWITCH:
                category = SMRPG_NORMAL_RTA_TURBO;
                break;
            case GAME_ID_HM_FOMT:
                if (streamTitle.contains("karen")) {
                    if (streamTitle.contains("glitchless")) {
                        category = HM_KAREN_GLITCHLESS;
                    } else {
                        category = HM_KAREN_GLITCHED;
                    }
                }
                break;
        }
    
        if (category == null) {
            twitchApi.channelMessage("Unknown WR");
            return;
        }
        twitchApi.channelMessage(String.format(
                "@%s %s",
                messageEvent.getUser().getName(),
                srcApi.getWr(category)
        ));
    }
}