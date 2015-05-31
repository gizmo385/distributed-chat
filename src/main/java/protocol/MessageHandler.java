import java.io.Serializable;

@FunctionalInterface
public interface MessageHandler {
    /**
     * This method will be called when a message has been recieved. The usefulness of many
     * listeners * extends mostly to areas when multiple message types will be handled (files,
     * audio, etc). This means that audio, files, regular messages, etc. could all be handled by
     * separate listeners.
     */
    public <E extends Serializable> void recieveMessage(Message<E> message);
}
