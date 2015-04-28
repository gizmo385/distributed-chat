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

    // Clients and rooms on the server
    private static int userId = 0;
    private static int roomId = 0;
    private Map<Integer, ClientHandler> clientConnections;


    public Server(int portNumber) {
        this.portNumber = portNumber;
        this.clientConnections = new HashMap<>();

        // Bind the server socket
        try {
            this.serverSocket = new ServerSocket(portNumber);
        } catch( IOException ioe ) {
            System.err.printf("Error while attempting to open server on port %d\n", portNumber);
            ioe.printStackTrace();
        }
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

                // Create a message notifying the client that they have arrived
                Message<Integer> loginConfirmation = new Message<>("Server", -1, userId,
                        MessageType.LOGIN_NOTIFICATION);

                // Create the client handler and send the login confirmation to them
                ClientHandler client = new ClientHandler(newClient, userId);
                client.sendMessage(loginConfirmation);
                client.start();

                // Add the client to the global client table
                clientConnections.put(userId, client);
                userId++;

            } catch( IOException ioe ) {
                System.err.printf("Error attempting to accept client on port %d\n", portNumber);
                ioe.printStackTrace();
            }
        }
    }

    private class ClientHandler extends Thread {
        // Client information
        private int userId;

        // Socket and stream
        private Socket clientSocket;
        private ObjectInputStream readFromClient;
        private ObjectOutputStream writeToClient;

        public ClientHandler(Socket clientSocket, int userId) {
            this.userId = userId;
            this.clientSocket = clientSocket;

            // Open the inputstream on the client
            try {
                this.readFromClient= new ObjectInputStream(clientSocket.getInputStream());
                this.writeToClient= new ObjectOutputStream(clientSocket.getOutputStream());
            } catch( IOException ioe ) {
                System.err.printf("Error while opening streams for client!\n");
                ioe.printStackTrace();
            }
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
            // Block until we recieve a message, then pass that message on to the proper rooms
            while( true ) {
                try {
                    Message<?> messageRecieved = (Message<?>)this.readFromClient.readObject();

                    // TODO: Distribute message to the rooms
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
