import java.util.Scanner;

import java.io.Serializable;

public class ChatClient {
    public static void main( String[] args ) {
        if( args.length < 3 ) {
            System.err.println("Usage: java ChatClient <clientName> <hostname> <portNumber>");
            System.exit(1);
        }

        // Read in command line arguments
        String clientName = args[0];
        String hostname = args[1];
        int portNumber = Integer.parseInt(args[2]);

        Client client = new Client( clientName, hostname, portNumber );
        client.setDefaultMessageHandler(ChatClient::printMessageToConsole);

        Scanner input = new Scanner( System.in );
        while( true ) {
            String text = input.nextLine();

            // Send the message
            Message<String> message = new Message<>(clientName, 0, text, MessageType.CHAT);
            client.writeMessage(message);
        }
    }

    private static <E extends Serializable> void printMessageToConsole(Message<E> message) {
        System.out.printf("%s: %s\n", message.getSender(), message.getContents());
    }
}
