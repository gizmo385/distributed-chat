import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;

import java.util.List;
import java.util.ArrayList;
import java.text.DecimalFormat;

public class SettingsDialog extends JDialog implements DocumentListener {

    // Dialog settings
    private final int WIDTH = 300;
    private final int HEIGHT = 300;

    // Components
    private JTextField name, hostname, portNumber, messageBouncerLimit, pttKey;
    private JButton save, exit;
    private JFrame parent;

    private List<JTextField> requiredFields;

    private ClientSettings settings;
    private boolean done;

    public SettingsDialog(JFrame parent) {
        super(parent, "Settings", true);
        this.done = false;
        this.parent = parent;
        this.settings = ClientSettings.loadSettings();
        this.requiredFields = new ArrayList<>();

        initDialog();
        initComponents();

        validateFields();
    }

    public SettingsDialog() {
        this(null);
    }

    private void initDialog() {
        this.setSize(WIDTH, HEIGHT);
        this.setLocationRelativeTo(this.parent);
        this.setLayout(new FlowLayout());
        this.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
    }

    private void initComponents() {
        DecimalFormat onlyPositiveIntegers = new DecimalFormat("#0");

        // Entering in your client name
        this.name = new JTextField(15);
        this.name.setText(this.settings.getClientName());
        JPanel namePanel = new JPanel();
        namePanel.add(new JLabel("Name: "));
        namePanel.add(this.name);

        // Entering in the hostname for the server
        this.hostname = new JTextField(15);
        this.hostname.setText(this.settings.getHostname());
        JPanel hostnamePanel = new JPanel();
        hostnamePanel.add(new JLabel("Hostname: "));
        hostnamePanel.add(this.hostname);

        // Entering in the server's port number
        this.portNumber = new JFormattedTextField(onlyPositiveIntegers);
        this.portNumber.setColumns(15);
        this.portNumber.setText(String.valueOf(this.settings.getPortNumber()));
        JPanel portNumberPanel = new JPanel();
        portNumberPanel.add(new JLabel("Port Number: "));
        portNumberPanel.add(this.portNumber);

        // Entering in the max number of messages a bouncer will send you
        this.messageBouncerLimit = new JFormattedTextField(onlyPositiveIntegers);
        this.messageBouncerLimit.setColumns(15);
        this.messageBouncerLimit.setText(String.valueOf(this.settings.getMessageBouncerLimit()));
        JPanel bouncerLimitPanel = new JPanel();
        bouncerLimitPanel.add(new JLabel("Bouncer Limit: "));
        bouncerLimitPanel.add(this.messageBouncerLimit);

        // Entering the push-to-talk key
        this.pttKey = new JTextField(15);
        this.pttKey.setEditable(false);
        this.pttKey.setText(KeyEvent.getKeyText(settings.getTouchToTalkKey()));
        this.pttKey.addKeyListener( new KeyAdapter() {
            @Override public void keyPressed(KeyEvent ke) {
                settings.setTouchToTalkKey(ke.getKeyCode());
                pttKey.setText(KeyEvent.getKeyText(ke.getKeyCode()));
            }
        });
        JPanel pttPanel = new JPanel();
        pttPanel.add(new JLabel("Push-to-talk key: "));
        pttPanel.add(pttKey);

        // Dialog buttons
        JPanel buttonPanel = new JPanel();
        this.save = new JButton("Save and Exit");
        this.save.addActionListener(ae -> {
            updateSettings();
            ClientSettings.saveSettings(settings);
            dispose();
            this.done = true;
        });
        this.exit = new JButton("Exit");
        this.exit.addActionListener(ae -> {
            dispose();
            this.done = true;
        });
        buttonPanel.add(this.save);
        buttonPanel.add(this.exit);

        setRequiredField(this.name);
        setRequiredField(this.hostname);
        setRequiredField(this.portNumber);
        setRequiredField(this.messageBouncerLimit);

        // Add the jpanels
        add(namePanel);
        add(hostnamePanel);
        add(portNumberPanel);
        add(bouncerLimitPanel);
        add(pttPanel);
        add(buttonPanel);
    }

    private void updateSettings() {
        settings.setClientName(this.name.getText());
        settings.setHostname(this.hostname.getText());
        settings.setPortNumber(Integer.parseInt(this.portNumber.getText()));
        settings.setMessageBouncerLimit(Integer.parseInt(this.messageBouncerLimit.getText()));
    }

    private void setRequiredField(JTextField field) {
        this.requiredFields.add(field);
        field.getDocument().addDocumentListener(this);
    }

    private void validateFields() {
        // Ensure all required fields are non-empty
        for( JTextField field : this.requiredFields ) {
            String text = field.getText().trim();

            if( text.isEmpty() ) {
                this.save.setEnabled(false);
                return;
            }
        }

        try {
            int bounceLimit = Integer.parseInt(messageBouncerLimit.getText());
            int port = Integer.parseInt(portNumber.getText());

            if( bounceLimit < 0 || port < 0 ) {
                this.save.setEnabled(false);
                return;
            }

        } catch( NumberFormatException nfe ) {
            this.save.setEnabled(false);
            return;
        }

        this.save.setEnabled(true);
    }

    /* Document listener implementation */
    public void changedUpdate(DocumentEvent de) { validateFields(); }
    public void insertUpdate(DocumentEvent de) { validateFields(); }
    public void removeUpdate(DocumentEvent de) { validateFields(); }
}
