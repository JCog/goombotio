package listeners.commands;

import com.github.twitch4j.chat.events.channel.ChannelMessageEvent;
import org.apache.commons.io.input.Tailer;
import org.apache.commons.io.input.TailerListener;
import org.apache.commons.io.input.TailerListenerAdapter;
import org.apache.commons.lang.SystemUtils;
import util.CommonUtils;
import util.TwitchApi;
import util.TwitchUserLevel.USER_LEVEL;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class YoutubeLinkListener extends CommandBase {
    private static final CommandType COMMAND_TYPE = CommandType.PREFIX_COMMAND;
    private static final USER_LEVEL MIN_USER_LEVEL = USER_LEVEL.BROADCASTER;
    private static final int COOLDOWN = 0;
    private static final CooldownType COOLDOWN_TYPE = CooldownType.GLOBAL;
    private static final String PATTERN_YTLINK = "!ytlink";
    private static final String SCRIPT_LOCATION = "/home/ubuntu/goombotio/chat.sh";
    private static final String LOG_PATH = "/home/ubuntu/goombotio/yt_chat_logs/";
    private static final String CHAT_FINISHED = "[INFO] Finished retrieving chat messages.";
    private static final Pattern PATTERN_CHAT = Pattern.compile(".* \\| (.*)");
    private static final Pattern PATTERN_ID = Pattern.compile("youtube.com/live/([a-zA-Z0-9_\\-]{1,11})");
    
    private final TailerListener listener;
    private final TwitchApi twitchApi;
    
    private Tailer tailer;
    private String filename;

    // runs a custom python script that retrieves YouTube chat and outputs it to a file, then tails that file and
    // relays it through whispers to the streamer. pretty hacky, but options for YouTube chat are very limited. should
    // maybe find a pure-Java solution at some point.
    // https://github.com/xenova/chat-downloader
    
    public YoutubeLinkListener(CommonUtils commonUtils) {
        super(COMMAND_TYPE, MIN_USER_LEVEL, COOLDOWN, COOLDOWN_TYPE, PATTERN_YTLINK);
        twitchApi = commonUtils.twitchApi();
        listener = new ChatRelay(twitchApi);
        
        tailer = null;
    }

    @Override
    protected void performCommand(String command, USER_LEVEL userLevel, ChannelMessageEvent messageEvent) {
        String[] splitMessage = messageEvent.getMessage().trim().split("\\s");
        if (splitMessage.length < 2) {
            twitchApi.channelMessage("ERROR: no link provided");
            return;
        }
        
        String link = splitMessage[1];
        Matcher matcher = PATTERN_ID.matcher(link);
        if (matcher.find()) {
            filename = LOG_PATH + matcher.group(1) + ".log";
        } else {
            twitchApi.channelMessage("ERROR: invalid link");
            return;
        }
        if (!SystemUtils.IS_OS_LINUX) {
            twitchApi.channelMessage("ERROR: this feature only works in a Linux environment");
            return;
        }
        
        new Thread(() -> {
            if (new File(filename).exists()) {
                // if the file already exists, it has an existing script instance monitoring it and we can just tail it
                System.out.println("Existing YouTube chat log detected, skipping chat script");
            } else {
                // start python script that outputs YouTube chat to file
                Process proc;
                try {
                    proc = Runtime.getRuntime().exec(SCRIPT_LOCATION + " " + link);
                } catch (java.io.IOException e) {
                    twitchApi.channelMessage("ERROR: IOException attempting to run script");
                    return;
                }
                
                try {
                    proc.waitFor();
                } catch (InterruptedException e) {
                    twitchApi.channelMessage("ERROR: Script was interrupted");
                    return;
                }
            }
            
            // tail and parse the file to mirror to Twitch whispers
            if (tailer != null) {
                tailer.close();
            }
            tailer = Tailer.builder()
                    .setFile(filename)
                    .setTailerListener(listener)
                    .setTailFromEnd(true)
                    .get();
            twitchApi.sendWhisper(twitchApi.getStreamerUser().getId(), "Now monitoring " + matcher.group(1));
        }).start();
    }
    
    private class ChatRelay extends TailerListenerAdapter {
        private final TwitchApi twitchApi;
        public ChatRelay(TwitchApi twitchApi) {
            this.twitchApi = twitchApi;
        }
        
        @Override
        public void handle(String line) {
            if (line.equals(CHAT_FINISHED)) {
                tailer.close();
                tailer = null;
                System.out.println("Finished monitoring YouTube chat");
            }
            Matcher matcher = PATTERN_CHAT.matcher(line);
            if (matcher.find()) {
                twitchApi.sendWhisper(
                        twitchApi.getStreamerUser().getId(),
                        "[CHAT] " + matcher.group(1)
                );
            } else if (line.startsWith("[INFO]")) {
                twitchApi.sendWhisper(twitchApi.getStreamerUser().getId(), line);
            }
        }
    }
}
