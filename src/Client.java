import java.net.Socket;
import java.io.IOException;

public class Client {
    // Connection information
    private Socket socket;
    private String hostname;
    private int portNumber;

    // Streaming information
    private ObjectOutputStream writeToServer;
    private ObjectInputStream readFromServer;

    public Client(String hostname, int portNumber) {
        this.hostname = hostname;
        this.portNumber = portNumber;

        this.socket = new Socket(hostname, portNumber);
        this.writeToServer = new ObjectOutputStream(this.socket.getOutputStream());
        this.readFromServer = new ObjectInputStream(this.socket.getInputStream());
    }
}
