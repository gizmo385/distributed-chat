import java.io.*;

import java.awt.event.KeyEvent;

public class ClientSettings implements Serializable {

    private final static long serialVersionUID = 100L;

    public transient static final ClientSettings DEFAULT = new ClientSettings(System.getProperty("user.name"), 50, 0, KeyEvent.VK_NUMPAD0, "localhost", 1212);

    // Connection settings
    private String hostname;
    private int portNumber;

    // Client settings
    private String clientName;
    private int touchToTalkKey;

    // Bouncer settings
    private int messageBouncerLimit;
    private long lastLoginDate;

    public ClientSettings(String clientName, int messageBouncerLimit, long lastLoginDate, int touchToTalkKey, String hostname, int portNumber ) {
        this.clientName = clientName;
        this.messageBouncerLimit = messageBouncerLimit;
        this.lastLoginDate = lastLoginDate;
        this.touchToTalkKey = touchToTalkKey;
        this.hostname = hostname;
        this.portNumber = portNumber;
    }

    public static void saveSettings(ClientSettings settings) {
        File settingsFile = new File("settings.dat");

        try(ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(settingsFile))) {
            settingsFile.createNewFile();
            oos.writeObject(settings);
        } catch( IOException ioe ) {
            System.err.println("There was an error saving the settings!");
            ioe.printStackTrace();
        }

    }

    public static ClientSettings loadSettings() {
        File settingsFile = new File("settings.dat");

        try(ObjectInputStream ois = new ObjectInputStream(new FileInputStream(settingsFile))) {
            ClientSettings settings = (ClientSettings) ois.readObject();

            return settings;
        } catch(Exception e) { }

        return ClientSettings.DEFAULT;
    }

    /* GETTERS */
    public String getClientName() {
        return this.clientName;
    }

    public int getTouchToTalkKey() {
        return this.touchToTalkKey;
    }

    public int getMessageBouncerLimit() {
        return this.messageBouncerLimit;
    }

    public long getLastLoginDate() {
        return this.lastLoginDate;
    }

    public String getHostname() {
        return this.hostname;
    }

    public int getPortNumber() {
        return this.portNumber;
    }

    /* SETTERS */
    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public void setTouchToTalkKey(int touchToTalkKey) {
        this.touchToTalkKey = touchToTalkKey;
    }

    public void setMessageBouncerLimit(int messageBouncerLimit) {
        this.messageBouncerLimit = messageBouncerLimit;
    }

    public void setLastLoginDate(long lastLoginDate) {
        this.lastLoginDate = lastLoginDate;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public void setPortNumber(int portNumber) {
        this.portNumber = portNumber;
    }
}
