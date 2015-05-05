import java.io.Serializable;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;

import java.util.Arrays;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class ChatClient extends JFrame {

    // Chat client components
    private String clientName, hostname;
    private int portNumber;
    private Client client;

    // GUI Components
    private final int WIDTH = 700;
    private final int HEIGHT = 400;
    private JTextArea messageHistory;
    private JTextField messageToSend;
    private JButton send, cancel, sendFile;

    private boolean recordingAudio = false;

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
        String connectionMessage = String.format("You have connected to %s:%d\n", hostname,
                portNumber);

        // Register some listeners
        this.client.registerHandler(MessageType.LOGIN_NOTIFICATION, this::displayWelcome);
        this.client.registerHandler(MessageType.CHAT, this::displayMessage);
        this.client.registerHandler(MessageType.FILE, this::receiveFile);
        this.client.registerHandler(MessageType.AUDIO, this::receiveAudio);
    }

    /**
     * Initializes all of the components present in the GUI. This includes buttons, text boxes,
     * action listeners, etc.
     */
    private void initComponents() {
        messageHistory = new JTextArea(20, 60);
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
        this.setSize(WIDTH, HEIGHT);
        this.setResizable(false);
        this.setTitle("Chat Client - " + clientName);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setLocationRelativeTo(null);

        KeyboardFocusManager manager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
        manager.addKeyEventDispatcher(new KeyDispatcher());

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

    private void sendAudio() {
        try {
            // Get the microphone
            AudioFormat format = new AudioFormat(8000.0f, 16, 1, true, true);
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
            TargetDataLine microphone = (TargetDataLine) AudioSystem.getLine(info);
            microphone.open(format);

            // Set up the output stream for the audio data
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            int CHUNK_SIZE = 1024;
            byte[] data = new byte[microphone.getBufferSize() / 5];
            microphone.start();

            // Start recording in a separte thread
            Thread recordingThread = new Thread(() -> {
                System.out.println("Starting recording...");
                while( recordingAudio ) {
                    int numBytesRead = microphone.read(data, 0, CHUNK_SIZE);
                    out.write(data, 0, numBytesRead);
                }
                System.out.println("Finished recording...");

                // Create new message
                byte[] audio = out.toByteArray();
                Message<byte[]> message = new Message<>(clientName, getCurrentRoom(), audio,
                    MessageType.AUDIO);

                System.out.println("Sending message...");
                client.writeMessage(message);
            });

            recordingThread.start();
        } catch( LineUnavailableException lue ) {
            System.err.println("Error while reading audio!");
            lue.printStackTrace();
        }
    }

    private <E extends Serializable> void receiveAudio(Message<E> message) {
        E messageContents = message.getContents();

        // Don't play audio sent by me
        if( message.getSenderId() == this.client.getClientId() ) {
            return;
        }

        // Add to the message history that an audio message was recieved
        String toDisplay = String.format("%s: [Audio Message]\n", message.getSender());
        messageHistory.append(toDisplay);

        if( messageContents instanceof byte[] ) {
            byte[] audioData = (byte[]) messageContents;

            // Get the speakers
            AudioFormat format = new AudioFormat(8000.0f, 16, 1, true, true);
            DataLine.Info dataLineInfo = new DataLine.Info(SourceDataLine.class, format);
            SourceDataLine speakers;

            try {
                speakers = (SourceDataLine) AudioSystem.getLine(dataLineInfo);
                speakers.open(format);
            } catch( LineUnavailableException lue ) {
                System.err.println("There was an error getting the audio output line!");
                lue.printStackTrace();
                return;
            }

            // Create a thread to play the audio
            Thread playThread = new Thread(() -> {
                speakers.start();
                speakers.write(audioData, 0, audioData.length);
                speakers.drain();
                speakers.stop();
                speakers.close();
            });
            playThread.start();
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

    private <E extends Serializable> void displayWelcome(Message<E> message) {
        String toDisplay = String.format("You have connected to %s:%d!\n", hostname, portNumber);
        SwingUtilities.invokeLater(() -> messageHistory.append(toDisplay));
    }

    /**
     * This is dispatcher which will handle sending audio events for audio messages.
     */
    private class KeyDispatcher implements KeyEventDispatcher {
        @Override
        public boolean dispatchKeyEvent(KeyEvent e) {
            // Ignore the event if it was fired by the message field
            if( e.getSource() == messageToSend ) {
                return false;
            }

            // Otherwise, check if it was a press or release
            if (e.getID() == KeyEvent.KEY_PRESSED) {
                // Ensure that the conditions are right to send an audio event
                if( e.getKeyCode() == KeyEvent.VK_NUMPAD0 && (!recordingAudio) ) {
                    recordingAudio = true;
                    sendAudio();
                }
            } else if (e.getID() == KeyEvent.KEY_RELEASED && e.getKeyCode() == KeyEvent.VK_NUMPAD0) {
                recordingAudio = false;
            }

            return false;
        }
    }

    public static void main( String[] args ) {
        ClientSettings settings;

        if( args.length > 2 ) {
            settings = ClientSettings.DEFAULT;
            settings.setClientName(args[0]);
            settings.setHostname(args[1]);
            settings.setPortNumber(Integer.parseInt(args[2]));
        } else {
            settings = ClientSettings.loadSettings();

            if( settings == ClientSettings.DEFAULT || settings == null) {

                SettingsDialog dialog = new SettingsDialog(null);
                dialog.setVisible(true);
                settings = ClientSettings.loadSettings();
            }
        }

        settings.setLastLoginDate(System.currentTimeMillis());
        ClientSettings.saveSettings(settings);

        // Create the chat client
        ChatClient cc = new ChatClient(settings.getClientName(), settings.getHostname(),
                settings.getPortNumber());
        cc.setVisible(true);
    }
}
