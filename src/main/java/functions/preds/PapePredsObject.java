package functions.preds;

import com.github.twitch4j.common.events.domain.EventUser;

public class PapePredsObject {
    private final EventUser twitchUser;
    private final PapePredsManager.Badge left, middle, right;

    public PapePredsObject(
            EventUser twitchUser,
            PapePredsManager.Badge left,
            PapePredsManager.Badge middle,
            PapePredsManager.Badge right) {
        this.twitchUser = twitchUser;
        this.left = left;
        this.middle = middle;
        this.right = right;
    }

    public EventUser getTwitchUser() {
        return twitchUser;
    }

    public PapePredsManager.Badge getLeft() {
        return left;
    }

    public PapePredsManager.Badge getMiddle() {
        return middle;
    }

    public PapePredsManager.Badge getRight() {
        return right;
    }
}
