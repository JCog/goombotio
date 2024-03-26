package listeners.commands;

import api.src.SrcApi;
import com.github.twitch4j.chat.events.channel.ChannelMessageEvent;
import com.github.twitch4j.helix.domain.Stream;
import com.netflix.hystrix.exception.HystrixRuntimeException;
import util.CommonUtils;
import util.TwitchApi;
import util.TwitchUserLevel.USER_LEVEL;

import static api.src.SrcEnums.BugFablesCategory.*;
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
    private static final String GAME_ID_SUNSHINE = "6086";
    private static final String GAME_ID_PAPER_MARIO = "18231";
    private static final String GAME_ID_TTYD = "6855";
    private static final String GAME_ID_OOT = "11557";
    private static final String GAME_ID_SMRPG_SWITCH = "1675405846";

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
                    wrText = srcApi.getWr(BF_HUNDO);
                } else if (streamTitle.contains("glitchless")) {
                    wrText = srcApi.getWr(BF_GLITCHLESS);
                } else if (streamTitle.contains("bosses")) {
                    wrText = srcApi.getWr(BF_ALL_BOSSES);
                } else if (streamTitle.contains("chapters")) {
                    wrText = srcApi.getWr(BF_ALL_CHAPTERS);
                } else if (streamTitle.contains("mystery")) {
                    wrText = srcApi.getWr(BF_ANY_MYSTERY);
                } else if (streamTitle.contains("codes")) {
                    wrText = srcApi.getWr(BF_ANY_ALL_CODES);
                } else if (streamTitle.contains("dll")) {
                    wrText = srcApi.getWr(BF_ANY_DLL);
                } else {
                    wrText = srcApi.getWr(BF_ANY_PERCENT);
                }
                break;
            case GAME_ID_SUNSHINE:
                if (streamTitle.contains("all episodes")) {
                    wrText = srcApi.getWr(SMS_ALL_EPISODES);
                } else if (streamTitle.contains("79")) {
                    wrText = srcApi.getWr(SMS_SHINES_79);
                } else if (streamTitle.contains("96")) {
                    wrText = srcApi.getWr(SMS_SHINES_96);
                } else if (streamTitle.contains("120")) {
                    wrText = srcApi.getWr(SMS_SHINES_120);
                } else {
                    wrText = srcApi.getWr(SMS_ANY_PERCENT);
                }
                break;
            case GAME_ID_PAPER_MARIO:
                if (streamTitle.contains("any% (no peach warp)") || streamTitle.contains("any% (no pw)")) {
                    wrText = srcApi.getWr(PAPE_ANY_PERCENT_NO_PW);
                } else if (streamTitle.contains("any%") &&(streamTitle.contains("no ace") || streamTitle.contains("acephobic"))) {
                    wrText = srcApi.getWr(PAPE_ANY_PERCENT);
                } else if (streamTitle.contains("any% no rng") || streamTitle.contains("any% (no rng)")) {
                    wrText = srcApi.getWr(PAPE_ANY_NO_RNG);
                } else if (streamTitle.contains("all cards") && !streamTitle.contains("reverse")) {
                    wrText = srcApi.getWr(PAPE_ALL_CARDS);
                } else if (streamTitle.contains("all bosses")) {
                    wrText = srcApi.getWr(PAPE_ALL_BOSSES);
                } else if (streamTitle.contains("glitchless")) {
                    wrText = srcApi.getWr(PAPE_GLITCHLESS);
                } else if (streamTitle.contains("100%")) {
                    wrText = srcApi.getWr(PAPE_HUNDO);
                } else if (streamTitle.contains("reverse") && streamTitle.contains("all cards")) {
                    wrText = srcApi.getWr(PAPE_REVERSE_ALL_CARDS);
                } else if (streamTitle.contains("pig") || streamTitle.contains("\uD83D\uDC37") || streamTitle.contains("oink")) {
                    wrText = srcApi.getWr(PAPE_PIGGIES);
                } else if (streamTitle.contains("all bloops")) {
                    wrText = srcApi.getWr(PAPE_ALL_BLOOPS);
                } else if (streamTitle.contains("chapter 1")) {
                    wrText = srcApi.getWr(PAPE_BEAT_CHAPTER_1);
                } else if (streamTitle.contains("soapcake") || streamTitle.contains("soap cake")) {
                    wrText = srcApi.getWr(PAPE_SOAP_CAKE);
                } else if (streamTitle.contains("mailman") || streamTitle.contains("amazon prime")) {
                    wrText = srcApi.getWr(PAPE_MAILMAN);
                } else if (streamTitle.contains("no major sequence breaks") || streamTitle.contains("nmsb")) {
                    wrText = srcApi.getWr(PAPE_NMSB);
                } else if (streamTitle.contains("swop")) {
                    wrText = srcApi.getWr(PAPE_STOP_N_SWOP);
                } else {
                    wrText = srcApi.getWr(PAPE_ANY_PERCENT);
                }
                break;
            case GAME_ID_TTYD:
                if (streamTitle.contains("crystal stars")) {
                    wrText = srcApi.getWr(TTYD_ALL_CRYSTAL_STARS);
                } else if (streamTitle.contains("100%") || streamTitle.contains("hundo")) {
                    wrText = srcApi.getWr(TTYD_HUNDO);
                } else if (streamTitle.contains("glitchless")) {
                    wrText = srcApi.getWr(TTYD_GLITCHLESS);
                } else if (streamTitle.contains("collectibles")) {
                    wrText = srcApi.getWr(TTYD_ALL_COLLECTIBLES);
                } else if (streamTitle.contains("upgrades")) {
                    wrText = srcApi.getWr(TTYD_MAX_UPGRADES);
                } else {
                    wrText = srcApi.getWr(TTYD_ANY_PERCENT);
                }
                break;
            case GAME_ID_OOT:
                if (streamTitle.contains("swop")) {
                    wrText = srcApi.getWr(PAPE_STOP_N_SWOP);
                } else if (streamTitle.contains("glitchless")){
                    wrText = srcApi.getWr(OOT_GLITCHLESS_AMQ);
                } else {
                    wrText = srcApi.getWr(OOT_ANY_PERCENT);
                }
                break;
            case GAME_ID_SMRPG_SWITCH:
                wrText = srcApi.getWr(SMRPG_NORMAL_RTA_TURBO);
                break;
        }
    
        twitchApi.channelMessage(String.format("@%s %s", messageEvent.getUser().getName(), wrText));
    }
}