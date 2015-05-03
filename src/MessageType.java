public enum MessageType {
    /*********************************************************
     * SERVER COMMANDS
     ********************************************************/

    /**
     * Creates a new room on the server
     */
    CREATE_ROOM("createroom"),

    /*********************************************************
     * SERVER RESPONSES
     ********************************************************/

    /**
     * Notifies the user that a room was created successfully
     */
    CREATE_ROOM_SUCCESS,

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
    AUTHENTICATION,

    /*********************************************************
     * CLIENT MESSAGES
     ********************************************************/

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
    LOGIN_INFORMATION;

    String commandString;

    MessageType() {
        commandString = "";
    }

    MessageType(String stringRepresentation) {
        commandString = stringRepresentation;
    }

    public String getCommandString() {
        return this.commandString;
    }

    public static MessageType getTypeFromCommand(String commandString) {
        for ( MessageType type : MessageType.values() ) {
            if ( type.getCommandString().equals(commandString) ) {
                return type;
            }
        }
        return null;
    }

}
