import java.net.ServerSocket;
import java.net.Socket;

import java.io.IOException;
import java.io.Serializable;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import java.util.Map;
import java.util.HashMap;

public class Server {
    // Server information
    private ServerSocket serverSocket;
    private int portNumber;

    // The room ID for the global chat room
    public final int GLOBAL_ROOM_ID;

    // Clients and rooms on the server
    private static int userIdCounter = 0;
    private static int roomId = 0;
    private Map<Integer, ClientHandler> clientConnections;
    private Map<Integer, Room> rooms;


    public Server(int portNumber) {
        this.portNumber = portNumber;
        this.clientConnections = new HashMap<>();
        this.rooms = new HashMap<>();

        // Bind the server socket
        try {
            this.serverSocket = new ServerSocket(portNumber);
        } catch( IOException ioe ) {
            System.err.printf("Error while attempting to open server on port %d\n", portNumber);
            ioe.printStackTrace();
        }

        // Create the global chat room that all users can join
        Room globalRoom = new Room();
        GLOBAL_ROOM_ID = globalRoom.getId();
        this.rooms.put(GLOBAL_ROOM_ID, globalRoom);
    }

    /**
     * Will block listening for incoming clients. Upon accepting a client, it will send the client
     * a user id to confirm that they are logged into the server.
     */
    public void startAccepting() {
        while( true ) {
            try {
                // Wait until a new client has arrived
                Socket newClient = serverSocket.accept();

                // Create a handler for that client
                ClientHandler client = new ClientHandler(newClient);

                // Create a message notifying the client that they have arrived
                Message<Integer> loginConfirmation = new Message<>("Server", -1, client.userId,
                        MessageType.LOGIN_NOTIFICATION);

                /*
                 * Send login confirmation to the client and begin listening for messages on
                 * a separate thread
                 */
                client.sendMessage(loginConfirmation);
                client.start();

                // Add the client to the global client table
                clientConnections.put(client.userId, client);

                // Add the client to the global room
                joinGlobalRoom(client.userId);

            } catch( IOException ioe ) {
                System.err.printf("Error attempting to accept client on port %d\n", portNumber);
                ioe.printStackTrace();
            }
        }
    }

    public void joinGlobalRoom(int userId) {
        this.rooms.get(GLOBAL_ROOM_ID).addUser(userId);
    }

    private class ClientHandler extends Thread {
        // Client information
        public final int userId;

        // Socket and stream
        private Socket clientSocket;
        private ObjectInputStream readFromClient;
        private ObjectOutputStream writeToClient;

        public ClientHandler(Socket clientSocket) {
            this.userId = userIdCounter;
            this.clientSocket = clientSocket;

            // Open the inputstream on the client
            try {
                this.readFromClient= new ObjectInputStream(clientSocket.getInputStream());
                this.writeToClient= new ObjectOutputStream(clientSocket.getOutputStream());
            } catch( IOException ioe ) {
                System.err.printf("Error while opening streams for client!\n");
                ioe.printStackTrace();
            }

            userIdCounter++;
        }

        public <E extends Serializable> void sendMessage(Message<E> messageToSend) {
            try {
                this.writeToClient.writeObject(messageToSend);
            } catch( IOException ioe ) {
                System.err.printf("Error writing to client!\n");
                ioe.printStackTrace();
            }
        }

        public void run() {
            // Block until we recieve a message
            while( true ) {
                try {
                    // Discover where the user is sending the message to
                    Message<?> messageRecieved = (Message<?>)this.readFromClient.readObject();
                    Room destination = rooms.get(messageRecieved.getDestination());

                    if( destination != null ) {
                        /*
                         * If the room the user is sending to exists, distribute the message to all
                         * users who are currently in that room.
                         */
                        for( int userId : destination.getUsers() ) {
                            ClientHandler client = clientConnections.get(userId);
                            client.sendMessage(messageRecieved);
                        }
                    } else {
                        /*
                         * If the user tries to send a message to non-existent room, send them an
                         * error message in response
                         */
                        Message<String> errorMessage = new Message<String>("Server", -1,
                                String.format("%d is not a valid room id!",
                                    messageRecieved.getDestination()), MessageType.ERROR);

                        writeToClient.writeObject(errorMessage);
                    }
                } catch( IOException ioe ) {
                    System.err.printf("Error while reading message from client!\n");
                    ioe.printStackTrace();
                } catch( ClassNotFoundException cnfe ) {
                    System.err.printf("Invalid message class recieved over socket!\n");
                    cnfe.printStackTrace();
                }
            }
        }
    }
}
