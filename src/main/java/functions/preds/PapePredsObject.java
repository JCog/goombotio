package functions.preds;

public class PapePredsObject {
    private final String userId;
    private final String displayName;
    private final PapePredsManager.Badge left;
    private final PapePredsManager.Badge middle;
    private final PapePredsManager.Badge right;

    public PapePredsObject(
            String userId,
            String displayName,
            PapePredsManager.Badge left,
            PapePredsManager.Badge middle,
            PapePredsManager.Badge right
    ) {
        this.userId = userId;
        this.displayName = displayName;
        this.left = left;
        this.middle = middle;
        this.right = right;
    }

    public String getUserId() {
        return userId;
    }
    
    public String getDisplayName() {
        return displayName;
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
