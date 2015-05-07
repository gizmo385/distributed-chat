public enum MessageType {
    /*********************************************************
     * SERVER COMMANDS
     ********************************************************/

    /**
     * Creates a new room on the server
     */
    CREATE_ROOM("createroom"),

    /**
     * Joins a room which has already been created
     */
    JOIN_ROOM("joinroom"),

    /**
     * Joins a room which has already been created
     */
    LEAVE_ROOM("leaveroom"),

    /**
     * Lists the users in the current room
     */
    LIST_USERS("listusers"),

    /**
     * Lists the currently active rooms in the server
     */
    LIST_ROOMS("listrooms"),

    /*********************************************************
     * SERVER RESPONSES
     ********************************************************/

    /**
     * This message is sent to a client to notify them of a successful connection
     */
    CONNECTION_SUCCESS,

    /**
     * This is a message sent by the server that notifies clients that they have logged in
     * successfully.
     */
    LOGIN_SUCCESS,

    /**
     * This is a message sent by the server that notifies clients that their login was
     * unsuccessful
     */
    LOGIN_FAILURE,

    /**
     * Notifies the user that a room can be joined
     */
    JOIN_ROOM_SUCCESS,

    /**
     * Notifies the user that the room could not be joined (does not exist, private, etc.)
     */
    JOIN_ROOM_FAILURE,

    /**
     * Notifies the user that a room can be joined
     */
    LEAVE_ROOM_SUCCESS,

    /**
     * Notifies the user that the room could not be joined (does not exist, private, etc.)
     */
    LEAVE_ROOM_FAILURE,

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
