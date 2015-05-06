public class ChatServer {
    public static void main( String[] args ) {
        if( args.length < 1 ) {
            System.err.println("Usage: java ChatServer <portNumber>");
            System.exit(1);
        }

        int portNumber = Integer.parseInt(args[0]);
        Server server = new Server(portNumber);
        server.startAccepting();
    }
}
