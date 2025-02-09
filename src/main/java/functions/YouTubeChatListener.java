package functions;

import org.apache.commons.io.input.Tailer;
import org.apache.commons.io.input.TailerListener;
import org.apache.commons.io.input.TailerListenerAdapter;
import util.CommonUtils;
import util.TwitchApi;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class YouTubeChatListener {
    private static final Pattern chatPattern = Pattern.compile("[0-9]{4}-[0-9]{2}-[0-9]{2} [0-9]{2}:[0-9]{2}:[0-9]{2} \\| (.*)");
    private final TwitchApi twitchApi;
    private final TailerListener listener;
    
    private Tailer tailer;
    
    public YouTubeChatListener(CommonUtils commonUtils) {
        twitchApi = commonUtils.twitchApi();
        listener = new ChatRelay(twitchApi);
        
        tailer = null;
    }
    
    public void readChat(String filename) {
        if (tailer != null) {
            tailer.close();
        }
        tailer = Tailer.builder()
                .setFile(filename)
                .setTailerListener(listener)
                .setTailFromEnd(true)
                .get();
    }
    
    public static class ChatRelay extends TailerListenerAdapter {
        private final TwitchApi twitchApi;
        public ChatRelay(TwitchApi twitchApi) {
            this.twitchApi = twitchApi;
        }
        
        @Override
        public void handle(String line) {
            Matcher matcher = chatPattern.matcher(line);
            if (matcher.find()) {
                twitchApi.sendWhisper(
                        twitchApi.getStreamerUser().getId(),
                        "YT Chat - " + matcher.group(1)
                );
            }
        }
    }
}
