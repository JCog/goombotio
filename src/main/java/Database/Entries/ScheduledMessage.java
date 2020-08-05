package Database.Entries;

public class ScheduledMessage {
    private final String id;
    private final String message;
    private final int weight;

    public ScheduledMessage(String id, String message, int weight) {
        this.id = id;
        this.message = message;
        this.weight = weight;
    }

    public String getId() {
        return id;
    }

    public String getMessage() {
        return message;
    }

    public int getWeight() {
        return weight;
    }
}
