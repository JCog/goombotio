package listeners.events;

import com.gikk.twirk.events.TwirkListener;
import com.gikk.twirk.types.twitchMessage.TwitchMessage;
import com.gikk.twirk.types.users.TwitchUser;
import util.TwirkInterface;

public class PyramidListener implements TwirkListener {
    private enum STATE {
        NONE,
        RISING,
        FALLING
    }

    private static final String INTERRUPT_EMOTE = "jcogSmile";
    private static final String TROLL_MESSAGE = "you thought OpieOP";
    private static final int MIN_HEIGHT = 3;
    private static final int TRIGGER_HEIGHT = 2;

    private final TwirkInterface twirk;

    private STATE state;
    private String pattern;
    private long userId;
    private int height;

    public PyramidListener(TwirkInterface twirk) {
        this.twirk = twirk;
        resetState();
    }

    @Override
    public void onPrivMsg(TwitchUser sender, TwitchMessage message) {
        String[] splitMessage = message.getContent().split(" ");

        if (splitMessage.length == 1) {
            state = STATE.RISING;
            pattern = splitMessage[0];
            userId = sender.getUserID();
            height = 1;
            return;
        }

        switch (state) {
            case RISING:
                //correct user, patterns are all the same, pattern is the correct one
                if (userId == sender.getUserID() && allPatternsEqual(pattern, splitMessage)) {
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
                if (userId == sender.getUserID() && allPatternsEqual(pattern, splitMessage)) {
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
        if (pattern.equals(INTERRUPT_EMOTE)) {
            twirk.channelMessage(TROLL_MESSAGE);
        }
        else {
            twirk.channelMessage(INTERRUPT_EMOTE);
        }
    }

    private void resetState() {
        state = STATE.NONE;
        pattern = "";
        userId = -1;
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