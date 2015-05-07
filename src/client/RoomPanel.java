import javax.swing.*;
import java.awt.*;

public class RoomPanel extends JPanel {

    private int roomId;
    private Dimension dimension;
    private JScrollPane scrollPane;
    private JTextArea chatWindow;
    private JList<String> userList;

    public RoomPanel(int roomId, int width, int height) {
        this.dimension = new Dimension(width, height);
        initComponents();
        this.roomId = roomId;
        this.setPreferredSize(this.dimension);
    }

    private void initComponents() {
        this.chatWindow = new JTextArea();
        this.chatWindow.setEditable(false);
        this.chatWindow.setLineWrap(true);

        this.scrollPane = new JScrollPane(chatWindow);
        this.scrollPane.setPreferredSize(this.dimension);
        this.scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        this.add(scrollPane);
    }

    public void append(String message) {
        this.chatWindow.append(message);
        JScrollBar sb = this.scrollPane.getVerticalScrollBar();
        sb.setValue(sb.getMaximum());
    }
}
