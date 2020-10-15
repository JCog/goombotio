package functions.preds;

import com.gikk.twirk.types.users.TwitchUser;

public class PapePredsObject {
    private final TwitchUser twitchUser;
    private final PapePredsManager.Badge left, middle, right;

    public PapePredsObject(
            TwitchUser twitchUser,
            PapePredsManager.Badge left,
            PapePredsManager.Badge middle,
            PapePredsManager.Badge right) {
        this.twitchUser = twitchUser;
        this.left = left;
        this.middle = middle;
        this.right = right;
    }

    public TwitchUser getTwitchUser() {
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
