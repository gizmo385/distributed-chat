import javax.swing.*;

public class RoomPanel extends JPanel {

    private int roomId;
    private JScrollPane scrollPane;
    private JTextArea chatWindow;
    private JList<String> userList;

    public RoomPanel(int roomId) {
        initComponents();
        this.roomId = roomId;
    }

    private void initComponents() {
        this.chatWindow = new JTextArea(20,60);
        this.chatWindow.setEditable(false);
        this.chatWindow.setLineWrap(true);
        this.scrollPane = new JScrollPane(chatWindow);
        this.scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        this.add(scrollPane);
    }

    public void append(String message) {
        this.chatWindow.append(message);
    }
}
