import java.net.ServerSocket;
import java.net.Socket;

import java.io.IOException;
import java.io.Serializable;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import java.util.*;
import java.util.stream.Collectors;

public class Server {

    private static final String SERVER_NAME = "Server";
    // Server information
    private ServerSocket serverSocket;
    private int portNumber;

    // The room ID for the global chat room
    public final int GLOBAL_ROOM_ID;

    // The ID for the server
    public static final int SERVER_ID = -1;

    // Clients and rooms on the server
    private static int userIdCounter = 0;
    private static int roomId = 0;
    private Map<Integer, ClientHandler> clientConnections;
    private Map<Integer, Room> rooms;
    private Map<MessageType, List<MessageHandler>> handlers;
    private List<String> clientUsernames;

    public Server(int portNumber) {
        this.portNumber = portNumber;
        this.clientConnections = new HashMap<>();
        this.rooms = new HashMap<>();
        this.handlers = new HashMap<>();
        this.clientUsernames = new ArrayList<>();

        // Bind the server socket
        try {
            this.serverSocket = new ServerSocket(portNumber);
        } catch( IOException ioe ) {
            System.err.printf("Error while attempting to open server on port %d\n", portNumber);
            ioe.printStackTrace();
            System.exit(1);
        }

        registerHandler(MessageType.LOGIN_INFORMATION, this::loginUser);
        registerHandler(MessageType.CREATE_ROOM, this::createRoom);
        registerHandler(MessageType.JOIN_ROOM, this::joinRoom);
        registerHandler(MessageType.LEAVE_ROOM, this::leaveRoom);
        registerHandler(MessageType.LIST_USERS, this::listUsers);
        registerHandler(MessageType.LIST_ROOMS, this::listRooms);
        registerHandler(MessageType.PRIVATE_MESSAGE, this::privateMessage);

        // Create the global chat room that all users can join
        Room globalRoom = new Room("Global Room");
        GLOBAL_ROOM_ID = globalRoom.getId();
        this.rooms.put(GLOBAL_ROOM_ID, globalRoom);
    }

    /**
     * This will register an implementation of the MessageHandler interface as being able to handle
     * messages of a particular type.
     *
     * @param type The type of messages that this handler can work with
     * @param listener Provides the handler function which will be called when a message of the
     * specified type is received by the client.
     */
    public void registerHandler(MessageType type, MessageHandler listener) {
        List<MessageHandler> typeHandlers = this.handlers.get(type);

        if( typeHandlers == null ) {
            typeHandlers = new ArrayList<>();
        }

        typeHandlers.add(listener);

        this.handlers.put(type, typeHandlers);
    }

    /**
     * Will block listening for incoming clients. Upon accepting a client, it will send the client
     * a user id to confirm that they are logged into the server.
     */
    public void startAccepting() {
        System.out.printf("Server -> The server is now listening on %s:%d\n",
                this.serverSocket.getInetAddress().getHostName(), portNumber);

        while ( true ) {
            try {
                // Wait until a new client has arrived
                Socket newClient = serverSocket.accept();

                // Create a handler for that client
                ClientHandler client = new ClientHandler(newClient);
                client.start();


            } catch( IOException ioe ) {
                System.err.printf("Error attempting to accept client on port %d\n", portNumber);
                ioe.printStackTrace();
            }
        }
    }

    public void joinGlobalRoom(int userId) {
        this.rooms.get(GLOBAL_ROOM_ID).addUser(userId);
    }

    public <E extends Serializable> void sendMessageToRoom( Message<E> message, Room room ) {
        if( room != null & message != null ) {
            System.out.printf("%s -> %s(%d) [type = %s]: %s\n", message.getSender(),
                    room.getName(), room.getId(), message.getType(), message.getContents());

            for( int userId : room.getUsers() ) {
                ClientHandler ch = this.clientConnections.get(userId);

                if( ch != null ) {
                    ch.sendMessage(message);
                } else {
                    System.out.printf("Server -> No users in room %s", room.getName());
                }
            }
        }
    }

    /**
     * This will send a notification to all handlers that have registered themself as being able to
     * handle messages of a particular type.
     *
     * @param message The message that has been received by the client
     */
    public <E extends Serializable> void notifyHandlers(Message<E> message) {
        MessageType type = message.getType();

        if( this.handlers.containsKey(type) ) {
            List<MessageHandler> typeHandlers = this.handlers.get(type);
            typeHandlers.stream().forEach(h -> h.recieveMessage(message));
        } else {
            System.err.printf("Received message with unhandled type: %s\n", type);
        }
    }

    private <E extends Serializable> void createRoom(Message<E> message) {
        Room room = new Room((String)message.getContents());
        room.addUser(message.getSenderId());
        this.rooms.put(room.getId(), room);
        Message<String> response = new Message<>(SERVER_NAME, room.getId(), room.getName(), MessageType.JOIN_ROOM_SUCCESS);
        ClientHandler ch = clientConnections.get(message.getSenderId());
        System.out.printf("Server -> %s(%d) created room %s(%d)\n", message.getSender(), message.getSenderId(), message.getContents(), room.getId());
        ch.sendMessage(response);
    }

    private <E extends Serializable> void listUsers(Message<E> message) {
        Message<String> response;
        try {
            int roomId = Integer.parseInt((String) message.getContents());
            Room room = this.rooms.get(roomId);

            String users = room.getUsers().stream()
                .map( i -> clientConnections.get(i) )
                .map( ch -> ch.clientName )
                .collect(Collectors.joining(", "));

            response = new Message<>(SERVER_NAME, roomId, users, MessageType.CHAT);
        } catch( Exception e ) {
            response = new Message<>(SERVER_NAME, GLOBAL_ROOM_ID,
                    "/listusers does not take an argument!", MessageType.CHAT);
        }

        this.clientConnections.get(message.getSenderId()).sendMessage(response);
    }

    private <E extends Serializable> void privateMessage(Message<E> message) {
        ClientHandler sender = this.clientConnections.get(message.getSenderId());

        // Verify that the sender exists
        if( sender == null ) {
            return;
        }

        try {
            // Identify the recipient
            ClientHandler recipient = null;
            for( ClientHandler ch : this.clientConnections.values() ) {
                if( ch.clientName.equals( message.getContents() ) ) {
                    recipient = ch;
                    break;
                }
            }

            // Can't start a conversation with nobody
            if( recipient == null ) {
                Message<String> response = new Message<>(SERVER_NAME, GLOBAL_ROOM_ID,
                        "Can't pm nobody!!\n", MessageType.JOIN_ROOM_FAILURE);
                sender.sendMessage(response);
                return;
            }

            // Can't start a conversation with yourself
            if( recipient.clientName.equals(sender.clientName) ) {
                Message<String> response = new Message<>(SERVER_NAME, GLOBAL_ROOM_ID,
                        "Can't pm yourself!!\n", MessageType.JOIN_ROOM_FAILURE);
                sender.sendMessage(response);
                return;
            }

            // Create the room and add the users
            String title = String.format("Conversation between %s and %s", sender.clientName,
                    recipient.clientName);
            Room room = new Room(title);
            this.rooms.put(room.getId(), room);
            room.addUser(sender.userId);
            room.addUser(recipient.userId);

            // Notify sender
            Message<String> senderResponse = new Message<>(SERVER_NAME, room.getId(),
                    String.format("conversation with %s", recipient.clientName),
                    MessageType.JOIN_ROOM_SUCCESS);
            sender.sendMessage(senderResponse);

            // Notify recipient
            Message<String> recipientResponse = new Message<>(SERVER_NAME, room.getId(),
                    String.format("conversation with %s", sender.clientName),
                    MessageType.JOIN_ROOM_SUCCESS);
            recipient.sendMessage(recipientResponse);

        } catch(Exception e) {
            Message<String> response = new Message<>(SERVER_NAME, GLOBAL_ROOM_ID,
                    "Could not start conversation!\n", MessageType.JOIN_ROOM_FAILURE);
            sender.sendMessage(response);
        }
    }

    private <E extends Serializable> void listRooms(Message<E> message) {
        Message<String> response;
        try {
            int roomId = Integer.parseInt((String) message.getContents());
            String rooms = this.rooms.values().stream()
                .map(r -> String.format("%s(%d)", r.getName(), r.getId()))
                .collect(Collectors.joining(", "));

            response = new Message<>(SERVER_NAME, roomId, rooms, MessageType.CHAT);
        } catch( Exception e ) {
            response = new Message<>(SERVER_NAME, GLOBAL_ROOM_ID,
                    "/listrooms does not take an argument!", MessageType.CHAT);
        }

        this.clientConnections.get(message.getSenderId()).sendMessage(response);
    }

    private <E extends Serializable> void joinRoom(Message<E> message) {
        Room roomToJoin;
        Message<String> response;

        // Get the sender and contents
        ClientHandler ch = clientConnections.get(message.getSenderId());
        E contents = message.getContents();

        // Find the room
        try {
            // Get the room being joined
            int roomId = Integer.parseInt( (String)contents );
            roomToJoin = this.rooms.get(roomId);

            if( roomToJoin != null ) {
                // Add the user
                roomToJoin.addUser(message.getSenderId());
                this.rooms.put(roomId, roomToJoin);

                // Send the confirmation to the user
                response = new Message<>(SERVER_NAME, roomId, roomToJoin.getName(), MessageType.JOIN_ROOM_SUCCESS);
                System.out.printf("Server -> %s(%d) joined room %s(%d)\n", message.getSender(), message.getSenderId(), roomToJoin.getName(), roomId);

                // Notify everyone of the new client
                String joined = String.format("%s has joined the room %s!", ch.clientName, roomToJoin.getName());
                Message<String> joinedMessage = new Message<>(SERVER_NAME, roomToJoin.getId(), joined,
                        MessageType.CHAT);
                joinedMessage.setSenderId(-1);

                sendMessageToRoom(joinedMessage, roomToJoin);
            } else {
                // Create error message saying room couldn't be found
                String str = String.format("Could not find room with id %d!\n", roomId);
                response = new Message<>(SERVER_NAME, Message.SERVER_ID, str, MessageType.JOIN_ROOM_FAILURE);
            }
        } catch( Exception e ) {
            // Handle invalid input from user
            response = new Message<>(SERVER_NAME, roomId, "Must send a valid room id!\n",
                    MessageType.JOIN_ROOM_FAILURE);
        }

        // Send the response
        ch.sendMessage(response);
    }

    private <E extends Serializable> void leaveRoom(Message<E> message) {
        try {
            Room room = this.rooms.get(Integer.parseInt(message.getContents().toString()));
            int senderId = message.getSenderId();
            room.removeUser(senderId);
            Message<Integer> leaveRoomMessage = new Message<>(SERVER_NAME, SERVER_ID, room.getId(), MessageType.LEAVE_ROOM_SUCCESS);
            ClientHandler client = clientConnections.get(message.getSenderId());
            client.sendMessage(leaveRoomMessage);
            System.out.printf("Server -> %s(%d) has left room %s(%d)\n", message.getSender(), message.getSenderId(), room.getName(), room.getId());
            if (room.getUsers().size() == 0) {
                rooms.remove(room.getId());
                System.out.printf("Server -> Room %s is empty, removing\n", room.getName());
            } else {
                Message<String> disconnected = new Message<>(SERVER_NAME, room.getId(),
                        String.format("%s has disconnected from %s", client.clientName,
                                room.getName()), MessageType.CHAT);
                disconnected.setSenderId(-1);
                sendMessageToRoom(disconnected, room);
            }
        } catch (Exception e) {
            String errorMessage = String.format("Improperly formatted leaveroom command 'leaveroom %s'", message.getContents());
            Message<String> leaveRoomMessage = new Message<>(SERVER_NAME, SERVER_ID, errorMessage, MessageType.LEAVE_ROOM_FAILURE);
            clientConnections.get(message.getSenderId()).sendMessage(leaveRoomMessage);
            System.out.printf("Server -> Improperly formatted message of type %s from %s\n", message.getType(), clientConnections.get(message.getSenderId()).getName());
        }
    }

    private <E extends Serializable> void loginUser(Message<E> message) {
        ClientHandler ch = this.clientConnections.get(message.getSenderId());
        ch.validate(message);
    }

    private class ClientHandler extends Thread {
        // Client information
        public final int userId;
        public String clientName;

        // Socket and stream
        private Socket clientSocket;
        private ObjectInputStream readFromClient;
        private ObjectOutputStream writeToClient;

        public ClientHandler(Socket clientSocket) {
            this.userId = userIdCounter++;
            this.clientSocket = clientSocket;

            // Add the client to the global client table
            clientConnections.put(userId, this);

            // Open the inputstream on the client
            try {
                this.readFromClient = new ObjectInputStream(clientSocket.getInputStream());
                this.writeToClient = new ObjectOutputStream(clientSocket.getOutputStream());
            } catch( IOException ioe ) {
                System.err.printf("Error while opening streams for client!\n");
                ioe.printStackTrace();
            }
            Message<Integer> connectionSuccess = new Message<>(SERVER_NAME, SERVER_ID, userId, MessageType.CONNECTION_SUCCESS);
            sendMessage(connectionSuccess);
        }

        public void disconnect(boolean sendMessage) {
            for(Iterator<Room> iter = rooms.values().iterator(); iter.hasNext(); ) {
                Room room = iter.next();
                room.removeUser(this.userId);

                if( sendMessage ) {
                    // Notify all rooms that the user was in that this user has disconnected
                    Message<String> disconnected = new Message<>(SERVER_NAME, room.getId(),
                            String.format("%s has disconnected from %s", this.clientName,
                                room.getName()), MessageType.CHAT);
                    disconnected.setSenderId(-1);

                    sendMessageToRoom(disconnected, room);
                    clientUsernames.remove(this.clientName);
                }

                if ( room.getUsers().size() == 0 && room.getId() != GLOBAL_ROOM_ID ) {
                    iter.remove();
                    System.out.printf("Server -> Room %s is empty, removing\n", room.getName());
                }
            }
        }

        public <E extends Serializable> void sendMessage(Message<E> messageToSend) {
            try {
                this.writeToClient.writeObject(messageToSend);
            } catch( IOException ioe ) {
                disconnect(false);
            }
        }

        public void run() {

            // Block until we recieve a message
            while( true ) {
                try {
                    // Discover where the user is sending the message to
                    Message<?> messageRecieved = (Message<?>)this.readFromClient.readObject();
                    MessageType type = messageRecieved.getType();
                    int destination = messageRecieved.getDestination();

                    if ( destination == SERVER_ID ) {
                        notifyHandlers(messageRecieved);
                    } else {
                        Room destinationRoom = rooms.get(destination);
                        if( destinationRoom != null ) {
                            sendMessageToRoom(messageRecieved, destinationRoom);
                        } else {
                            Message<String> errorMessage = new Message<>(SERVER_NAME, -1,
                                    String.format("%d is not a valid room id!",
                                            messageRecieved.getDestination()), MessageType.ERROR);

                            writeToClient.writeObject(errorMessage);
                        }
                    }
                } catch( IOException ioe ) {
                    disconnect(true);
                    break;
                } catch( ClassNotFoundException cnfe ) {
                    System.err.printf("Invalid message class recieved over socket!\n");
                    cnfe.printStackTrace();
                }
            }
        }

        private <E extends Serializable> void validate(Message<E> connectionInfo) {
            String clientName = (String)connectionInfo.getContents();

            // Create message indicating either success or failure of validation
            Message<?> loginResponse;
            if ( clientUsernames.contains(clientName) ) {
                String errorString = "Username already exists\nPlease try again";
                loginResponse = new Message<>(SERVER_NAME, SERVER_ID, errorString,
                        MessageType.LOGIN_FAILURE);
                loginResponse.setSenderId(SERVER_ID);
            } else {
                this.clientName = clientName;
                loginResponse = new Message<>(SERVER_NAME, GLOBAL_ROOM_ID, userId,
                        MessageType.LOGIN_SUCCESS);
                loginResponse.setSenderId(SERVER_ID);
                joinServer();
            }

            // Send the response created above
            sendMessage(loginResponse);
        }

        private void joinServer() {
            clientUsernames.add(clientName);
            // Add the client to the global room
            joinGlobalRoom(userId);

            // Notify everyone of the new client
            String joined = String.format("%s has joined the server!", clientName);
            Message<String> joinedMessage = new Message<>(SERVER_NAME, GLOBAL_ROOM_ID, joined,
                    MessageType.CHAT);
            joinedMessage.setSenderId(-1);

            Room globalRoom = rooms.get(GLOBAL_ROOM_ID);
            sendMessageToRoom(joinedMessage, globalRoom);
        }

    }

}
