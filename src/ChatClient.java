import java.util.Scanner;

import java.io.Serializable;
import java.io.IOException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class ChatClient extends JFrame {

    // Chat client components
    private String clientName, hostname;
    private int portNumber;
    private Client client;

    // GUI Components
    private JTextArea messageHistory;
    private JTextField messageToSend;
    private JButton send, cancel, sendFile;

    /**
     * Creates a new chat client which will connect to the specified server.
     *
     * @param clientName The name associated with your messages
     * @param hostname The IP address of the server you are connecting to
     * @param portNumber The port number that the server you are connecting to is listening on
     */
    public ChatClient( String clientName, String hostname, int portNumber ) {
        this.clientName = clientName;
        this.hostname = hostname;
        this.portNumber = portNumber;

        initFrame();
        initComponents();

        this.client = new Client(clientName, hostname, portNumber);
        this.client.registerHandler(MessageType.CHAT, this::displayMessage);
        this.client.registerHandler(MessageType.FILE, this::receiveFile);
    }

    /**
     * Initializes all of the components present in the GUI. This includes buttons, text boxes,
     * action listeners, etc.
     */
    private void initComponents() {
        messageHistory = new JTextArea(40, 40);
        messageHistory.setEditable(false);
        add(messageHistory);

        messageToSend = new JTextField(15);
        messageToSend.addActionListener(ae -> sendChatMessage());
        add(messageToSend);

        send = new JButton("Send");
        send.addActionListener(ae -> sendChatMessage());
        add(send);

        cancel = new JButton("Cancel");
        cancel.addActionListener(ae -> messageToSend.setText(""));
        add(cancel);

        sendFile = new JButton("Send File");
        sendFile.addActionListener(ae -> sendFile() );
        add(sendFile);
    }

    /**
     * Initializes frame settings. This includes things like the size of the frame, layout managers,
     * and the like.
     */
    private void initFrame() {
        // Frame settings
        this.setSize(600, 700);
        this.setResizable(false);
        this.setTitle("Chat Client");
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        this.setLayout(new FlowLayout());
    }

    /**
     * Determines which room you are currently sending messages to.
     *
     * @return The ID for the room that messages are currently being sent to.
     */
    private int getCurrentRoom() {
        // TODO: Implement multiple rooms in the client
        return 0;
    }

    /**
     * Sends a standard CHAT message containing the text present in the text field.
     */
    private void sendChatMessage() {
        String message = this.messageToSend.getText();
        int currentRoom = getCurrentRoom();

        if( ! message.trim().isEmpty() ) {
            Message<String> m = new Message<>(clientName, currentRoom, message, MessageType.CHAT);
            this.client.writeMessage(m);
        }

        this.messageToSend.setText("");
    }

    /**
     * This function handles messages which contain file data and are downloaded and saved on your
     * local computer. The user will be able to cancel the saving of the file before it actually
     * saves
     *
     * @param message A message, presumably containing the bytes of the file as contents.
     */
    private <E extends Serializable> void receiveFile(Message<E> message) {
        System.out.printf("Received file message from id %d\n", message.getSenderId());
        // If I sent the file, don't make me download it
        if( message.getSenderId() == this.client.getClientId() ) {
            return;
        }

        // Ensure that the contents of the message is the byte array
        E messageContents = message.getContents();
        String sender = message.getSender();

        if( messageContents instanceof byte[] ) {
            // The user must confirm that they want to download the file
            int dialogAnswer = JOptionPane.showConfirmDialog(this,
                    String.format("%s has sent a file. Would you like to download it?", sender),
                    "File Download", JOptionPane.YES_NO_OPTION);

            if( dialogAnswer == JOptionPane.YES_OPTION ) {
                JFileChooser jfc = new JFileChooser();
                int returnVal = jfc.showSaveDialog(this);

                // They must choose where to save the file
                if( returnVal == JFileChooser.APPROVE_OPTION ) {
                    File file = jfc.getSelectedFile();

                    // Write the bytes to the file
                    try(FileOutputStream fos = new FileOutputStream(file)) {
                        fos.write((byte[]) messageContents);
                        fos.flush();
                    } catch( IOException ioe ) {
                        JOptionPane.showMessageDialog(this, "Error saving file!", "Error!",
                                JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        } else {
            JOptionPane.showMessageDialog(this, "Message contents must contain file data!",
                    "Error!", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * This function allows users to send a file to their current rooom. The user wil be allowed to
     * choose the file that they wish to send and it will be distributed across the current room
     * that they are viewing.
     */
    private void sendFile() {
        // Let the user choose a file to send
        int currentRoom = getCurrentRoom();
        JFileChooser jfc = new JFileChooser();
        int returnVal = jfc.showOpenDialog(this);

        // If the user chose an option, read the file into a byte array
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File file = jfc.getSelectedFile();
            byte[] fileBytes = new byte[(int) file.length()];

            // Read in the bytes for the file
            try( FileInputStream fis = new FileInputStream(file) ) {
                fis.read(fileBytes);
            } catch( IOException ioe ) {
                System.err.printf("Error reading from %s\n", file.getName());
                ioe.printStackTrace();
                return;
            }

            // Create and send the message
            Message<byte[]> fileMessage = new Message<>(clientName, currentRoom, fileBytes,
                    MessageType.FILE);

            client.writeMessage(fileMessage);
        }
    }

    /**
     * This handler is for simple chat messages and will append the contents of the message to the
     * text area for this room.
     *
     * @param message A message, presumably of type CHAT, which contains the object which will be
     * converted to a string and displayed in the message history text area.
     */
    private <E extends Serializable> void displayMessage(Message<E> message) {
        String toDisplay = String.format("%s: %s\n", message.getSender(), message.getContents());
        SwingUtilities.invokeLater(() -> messageHistory.append(toDisplay));
    }

    public static void main( String[] args ) {
        if( args.length < 3 ) {
            System.err.println("Usage: java ChatClient <clientName> <hostname> <portNumber>");
            System.exit(1);
        }

        // Read in command line arguments
        String clientName = args[0];
        String hostname = args[1];
        int portNumber = Integer.parseInt(args[2]);

        ChatClient cc = new ChatClient(clientName, hostname, portNumber);
        cc.setVisible(true);
    }
}
