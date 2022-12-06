package listeners.events;

import com.github.twitch4j.chat.events.channel.ChannelMessageEvent;
import com.github.twitch4j.common.events.domain.EventUser;
import listeners.TwitchEventListener;
import util.TwitchApi;

import java.util.Objects;

public class PyramidListener implements TwitchEventListener {
    private enum STATE {
        NONE,
        RISING,
        FALLING
    }

    private static final String INTERRUPT_EMOTE = "jcogSmile";
    private static final String TROLL_MESSAGE = "you thought OpieOP";
    private static final int MIN_HEIGHT = 3;
    private static final int TRIGGER_HEIGHT = 2;

    private final TwitchApi twitchApi;

    private STATE state;
    private String pattern;
    private String userId;
    private int height;

    public PyramidListener(TwitchApi twitchApi) {
        this.twitchApi = twitchApi;
        resetState();
    }

    @Override
    public void onPrivMsg(ChannelMessageEvent messageEvent) {
        EventUser sender = messageEvent.getUser();
        String[] splitMessage = messageEvent.getMessage().split(" ");

        if (splitMessage.length == 1) {
            state = STATE.RISING;
            pattern = splitMessage[0];
            userId = sender.getId();
            height = 1;
            return;
        }

        switch (state) {
            case RISING:
                //correct user, patterns are all the same, pattern is the correct one
                if (Objects.equals(userId, sender.getId()) && allPatternsEqual(pattern, splitMessage)) {
                    if (splitMessage.length == height + 1) {
                        height += 1;
                    }
                    else if (splitMessage.length == height - 1 && height >= MIN_HEIGHT) {
                        if (splitMessage.length == TRIGGER_HEIGHT) {
                            interruptPyramid();
                            resetState();
                        }
                        else {
                            state = STATE.FALLING;
                            height -= 1;
                        }
                    }
                    else {
                        resetState();
                    }
                }
                else {
                    resetState();
                }
                break;
            case FALLING:
                //correct user, patterns are all the same, pattern is the correct one
                if (Objects.equals(userId, sender.getId()) && allPatternsEqual(pattern, splitMessage)) {
                    if (splitMessage.length == TRIGGER_HEIGHT) {
                        interruptPyramid();
                        resetState();
                    }
                    else if (splitMessage.length == height - 1) {
                        height -= 1;
                    }
                    else {
                        resetState();
                    }
                }
                else {
                    resetState();
                }
                break;
        }
    }

    private void interruptPyramid() {
        if (pattern.startsWith(INTERRUPT_EMOTE)) {
            twitchApi.channelMessage(TROLL_MESSAGE);
        }
        else {
            twitchApi.channelMessage(INTERRUPT_EMOTE);
        }
    }

    private void resetState() {
        state = STATE.NONE;
        pattern = "";
        userId = "";
        height = 0;
    }

    private static boolean allPatternsEqual(String base, String[] patterns) {
        if (patterns.length == 0) {
            return false;
        }

        for (String pattern : patterns) {
            if (!pattern.equals(base)) {
                return false;
            }
        }
        return true;
    }
}