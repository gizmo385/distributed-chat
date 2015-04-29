public enum MessageType {
    /**
     * Messages being sent between clients in the application. The payload of this message should
     * be a Stringable type (has a useful toString method).
     */
    CHAT,

    /**
     * A file message is when one client shares a file with a room. The payload of this kind of
     * message would be the byte-data for the file.
     */
    FILE,

    /**
     * This is not going to be immediately implemented but refers to audio data being sent across
     * a room. The payload for this message would be the byte data for the audio.
     */
    AUDIO,

    /**
     * This is the type of message sent when a user is authenticating with the server. The payload
     * of this message is a String, representing the user's client name.
     */
    LOGIN_INFORMATION,

    /**
     * This is a message sent by the server that notifies clients that they have logged in
     * successfully. It will send a user ID to the client as well.
     */
    LOGIN_NOTIFICATION,

    /**
     * These are messages sent by the server that notify clients of errors that have occured,
     * potentially due to their input.
     */
    ERROR,

    /**
     * An authentication message is used in authenticating entry into a protected room.
     */
    AUTHENTICATION;
}
