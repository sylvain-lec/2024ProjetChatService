package fr.uga.miashs.dciss.chatservice.gui;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.ImageIcon;

public class Chat {

    private JFrame frame;
    private JPanel topPanel;
    private JPanel leftTopPanel;
    private JTextField searchField;
    private JPanel rightTopPanel;
    private JLabel profileLabel;

    private JPanel leftPanel;
    private JPanel rightPanel;
    private DefaultListModel<String> contactListModel; // Model to handle contacts
    private JList<String> contactList; // List of contacts
    private JTextArea chatArea; // Area where chat messages will be displayed
    private JTextField messageInput; // Field to input new messages
    private JButton sendButton; // Button to send messages

    public Chat() {
        initializeUI();
    }

    private void initializeUI() {
        frame = new JFrame("Chat");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);

        // Create the top panel
        topPanel = new JPanel(new BorderLayout());
        leftTopPanel = new JPanel();
        searchField = new JTextField(15);
        leftTopPanel.add(searchField);

        rightTopPanel = new JPanel();
        profileLabel = new JLabel("Profile", new ImageIcon("user_icon.png"), JLabel.LEFT); // Change "user_icon.png" to the actual path of your user icon
        rightTopPanel.add(profileLabel);

        topPanel.add(leftTopPanel, BorderLayout.WEST);
        topPanel.add(rightTopPanel, BorderLayout.EAST);
        frame.add(topPanel, BorderLayout.NORTH);

        // Split the frame into two panels
        JSplitPane splitPane = new JSplitPane();

        leftPanel = new JPanel();
        leftPanel.setLayout(new BorderLayout());

        // Initialize the contact list model and add dummy data
        contactListModel = new DefaultListModel<>();
        contactListModel.addElement("This Name");
        contactListModel.addElement("That Name");
        contactListModel.addElement("Another Name");
        contactListModel.addElement("Another Another Name");


        contactList = new JList<>(contactListModel);
        contactList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        contactList.setSelectedIndex(0);
        leftPanel.add(new JScrollPane(contactList), BorderLayout.CENTER);

        // Add listener for double-click on contact list
        contactList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    String selectedContact = contactList.getSelectedValue();
                    chatArea.append("Chat avec: " + selectedContact + "\n");
                }
            }
        });

        rightPanel = new JPanel();
        rightPanel.setLayout(new BorderLayout());

        chatArea = new JTextArea();
        chatArea.setEditable(false);
        rightPanel.add(new JScrollPane(chatArea), BorderLayout.CENTER);

        // Input area and send button at the bottom of the right panel
        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new BorderLayout());

        messageInput = new JTextField();
        inputPanel.add(messageInput, BorderLayout.CENTER);

        sendButton = new JButton("Envoyer");
        sendButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendMessage();
            }
        });
        inputPanel.add(sendButton, BorderLayout.EAST);

        // Add Enter key listener to send messages

        messageInput.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendMessage();
            }
        });

        rightPanel.add(inputPanel, BorderLayout.SOUTH);

        // Add the two panels to the split pane
        splitPane.setLeftComponent(leftPanel);
        splitPane.setRightComponent(rightPanel);

        frame.add(splitPane);
        frame.setVisible(true);
    }

    private void sendMessage() {
        String message = messageInput.getText().trim();
        if (!message.isEmpty()) {
            chatArea.append("Vous: " + message + "\n");  // Display the message in the chat area
            messageInput.setText("");  // Clear the text input field
            // Here, you would send the message to the server in a real application
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new Chat();
            }
        });
    }
}
