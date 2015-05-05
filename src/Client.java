import java.net.Socket;
import java.net.UnknownHostException;

import java.io.IOException;
import java.io.Serializable;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

public class Client {
    // Connection information
    private Socket socket;
    private String hostname;
    private int portNumber;
    private int clientId;

    // Streaming information
    private ObjectOutputStream writeToServer;
    private ObjectInputStream readFromServer;

    // Other information maintained by the client
    private String clientName;
    private Map<MessageType, List<MessageHandler>> handlers;
    private MessageHandler defaultHandler;

    public Client(String clientName, String hostname, int portNumber) {
        this.clientName = clientName;
        this.hostname = hostname;
        this.portNumber = portNumber;
        this.handlers = new HashMap<>();

        registerHandler(MessageType.CONNECTION_SUCCESS, this::validateUsername);

        // Open the connection to the server
        try {
            this.socket = new Socket(hostname, portNumber);
            this.writeToServer = new ObjectOutputStream(this.socket.getOutputStream());
            this.readFromServer = new ObjectInputStream(this.socket.getInputStream());

            // Start the reader thread
            ClientReader reader = new ClientReader(this.readFromServer);
            Thread readerThread = new Thread(reader);
            readerThread.start();

        } catch( UnknownHostException uhe ) {
            System.err.printf("Could not connect to %s:%d\n", hostname, portNumber);
            uhe.printStackTrace();
        } catch( IOException ioe ) {
            System.err.printf("Error openning streams to %s:%d\n", hostname, portNumber);
            ioe.printStackTrace();
        }
    }

    private <E extends Serializable> void validateUsername(Message<E> message) {
        setClientId(message);

        // Send client information to the server
        Message<String> loginInfo = new Message<>(clientName, Server.SERVER_ID, clientName,
                MessageType.LOGIN_INFORMATION);
        writeMessage(loginInfo);
    }

    private <E extends Serializable> void setClientId(Message<E> message) {
        if( message.getContents() instanceof Integer ) {
            this.clientId = (Integer) message.getContents();
        }
    }

    public int getClientId() {
        return this.clientId;
    }

    public <E extends Serializable> void writeMessage( Message<E> message ) {
        try {
            message.setSenderId(this.clientId);
            writeToServer.writeObject(message);
        } catch( IOException ioe ) {
            System.err.printf("Error writing message to %s:%d!\n", this.hostname, this.portNumber);
        }
    }

    /**
     * The default message handler will handle messages when no suitable handler can be found. If no
     * default handler is present, an the message will be discarded.
     *
     * @param handler An implementation of the MessageHandler interface that will handle messages no
     * handled by other handlers
     */
    public void setDefaultMessageHandler(MessageHandler handler) {
        this.defaultHandler = handler;
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
        } else if( this.defaultHandler != null ) {
            this.defaultHandler.recieveMessage(message);
        } else {
            System.err.printf("Received message with unhandled type: %s\n", type);
        }
    }

    /**
     * This is an implementation of the Runnable interface that will listen to the input stream of
     * a socket and notify any handler of messages which have arrived.
     */
    private class ClientReader implements Runnable {

        private ObjectInputStream serverRead;

        public ClientReader(ObjectInputStream serverRead) {
            this.serverRead = serverRead;
        }

        public void run() {
            while( true ) {
                try {
                    Message<?> message = (Message<?>)serverRead.readObject();

                    notifyHandlers(message);
                } catch( IOException ioe ) {
                    System.err.println("There was an error while reading from the server!");
                    ioe.printStackTrace();
                    System.exit(1);
                } catch( ClassNotFoundException cnfe ) {
                    System.err.printf("Invalid message read from %s:%d\n", hostname, portNumber);
                    cnfe.printStackTrace();
                }
            }
        }
    }
}
