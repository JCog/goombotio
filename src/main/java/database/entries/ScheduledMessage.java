package database.entries;

public class ScheduledMessage extends GenericMessage {
    private final String id;
    private final int weight;

    public ScheduledMessage(String id, String message, int weight) {
        super(message);
        this.id = id;
        this.weight = weight;
    }
    
    public String getId() {
        return id;
    }

    public int getWeight() {
        return weight;
    }
}
