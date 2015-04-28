import java.io.Serializable;

public final class Message<E extends Serializable> {
    private final String senderName;
    private final int destinationRoom;
    private final MessageType type;
    private final E messageContents;

    public Message(String from, int destination, E contents, MessageType type) {
        this.senderName = from;
        this.destinationRoom = destination;
        this.messageContents = contents;
        this.type = type;

    }

    public String getSender() {
        return this.senderName;
    }

    public int getDestination() {
        return this.destinationRoom;
    }

    public E getContents() {
        return this.messageContents;
    }

    public MessageType getType() {
        return this.type;
    }
}
