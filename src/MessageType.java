public enum MessageType {
    /**
     * Simple messages are chat messages being sent between clients in the application. The payload
     * of this message should be a Stringable type (has a useful toString method).
     */
    SIMPLE_MESSAGE,

    /**
     * A file message is when one client shares a file with a room. The payload of this kind of
     * message would be the byte-data for the file.
     */
    FILE_MESSAGE,

    /**
     * This is not going to be immediately implemented but refers to audio data being sent across
     * a room. The payload for this message would be the byte data for the audio.
     */
    AUDIO_MESSAGE,

    /**
     * This is a message sent by the server that notifies clients that they have logged in
     * successfully. It will send a user ID to the client as well.
     */
    LOGIN_NOTIFICATION,

    /**
     * An authentication message is used in authenticating entry into a protected room.
     */
    AUTHENTICATION_MESSAGE;
}
