import java.io.Serializable;

public final class Message<E extends Serializable> implements Serializable {

    private static final long serialVersionUID = 20L;

    private final String senderName;
    private final int destinationRoom;
    private final MessageType type;
    private final E messageContents;

    // This gets set by the server
    private int senderId;

    public Message(String from, int destination, E contents, MessageType type) {
        this.senderName = from;
        this.destinationRoom = destination;
        this.messageContents = contents;
        this.type = type;

    }

    public void setSenderId(int senderId) {
        this.senderId = senderId;
    }

    public int getSenderId() {
        return this.senderId;
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
