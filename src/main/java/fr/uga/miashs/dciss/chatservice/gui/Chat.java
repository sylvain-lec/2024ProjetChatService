package fr.uga.miashs.dciss.chatservice.gui;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.ImageIcon;
import javax.swing.text.*;

public class Chat {

    private JFrame frame;
    private JPanel topPanel;
    private JPanel leftTopPanel;
    private PlaceholderTextField searchField;
    private JPanel rightTopPanel;
    private JLabel profileLabel;

    private JPanel leftPanel;
    private JPanel rightPanel;
    private DefaultListModel<String> contactListModel;
    private JList<String> contactList;
    private JTextArea chatArea;
    private PlaceholderTextField messageInput;
    private JButton sendButton;
    private JComboBox<String> profileMenu;
    private JButton addContactButton;
    private JButton createGroupButton;

    public Chat() {
        initializeUI();
        customizeUIComponents();
        initializeButtons();
    }

    private void initializeButtons() {
        addContactButton = new JButton("Ajouter Contact");
        createGroupButton = new JButton("Créer Groupe");

        addContactButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String contactName = JOptionPane.showInputDialog(frame, "Entrez nom de contact:");
                if (contactName != null && !contactName.trim().isEmpty()) {
                    contactListModel.addElement(contactName);
                }
            }
        });

        createGroupButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String groupName = JOptionPane.showInputDialog(frame, "Entrez nom de groupe:");
                if (groupName != null && !groupName.trim().isEmpty()) {
                    System.out.println("Groupe a été créé: " + groupName);
                }
            }
        });

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttonPanel.add(addContactButton);
        buttonPanel.add(createGroupButton);

        // Adding the button panel below the contact list
        leftPanel.add(buttonPanel, BorderLayout.SOUTH);

        frame.validate();
        frame.repaint();
    }

    private void initializeUI() {
        frame = new JFrame("Chat");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);

        // Create the top panel
        topPanel = new JPanel(new BorderLayout());
        leftTopPanel = new JPanel();
        searchField = new PlaceholderTextField(20);
        searchField.setFocusable(false);
        searchField.setPlaceholder("Rechercher...");
        leftTopPanel.add(searchField);

        rightTopPanel = new JPanel();
        String[] profileOptions = {"Profile", "Log In", "Log Out"};
        profileMenu = new JComboBox<>(profileOptions);
        profileMenu.setFocusable(false);

        profileMenu.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String selectedOption = (String) profileMenu.getSelectedItem();
                // Handle the selected option
                switch (selectedOption) {
                    case "Votre Profile":
                        // Handle "View Profile" action
                        break;
                    case "Log In":
                        // Handle "Log In" action
                        break;
                    case "Log Out":
                        // Handle "Log Out" action
                        break;
                }
            }
        });
        rightTopPanel.add(profileMenu);
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

        JScrollPane contactListScrollPane = new JScrollPane(contactList);
        contactListScrollPane.setPreferredSize(new Dimension(200, 600));
        leftPanel.add(contactListScrollPane, BorderLayout.CENTER);
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

        messageInput = new PlaceholderTextField();
        messageInput.setPlaceholder("Vos messages...");

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
            // Here, send the message to the server in a real application
        }
    }

    private void customizeUIComponents() {
        // Personalization of the side bar
        leftPanel.setBackground(Color.WHITE);

        // Personalization of the contact list
        contactList.setFixedCellHeight(60); // Fixed height for each cell
        contactList.setCellRenderer(new CustomCellRenderer()); // Custom renderer for cells

        // Personalization of the chat area
        chatArea.setFont(new Font("SansSerif", Font.PLAIN, 16)); // Choose a readable font
        chatArea.setMargin(new Insets(10, 10, 10, 10)); // Inner margin for JTextArea

        // Personalization of the input area
        messageInput.setPreferredSize(new Dimension(0, 40)); // Height of the input area
        messageInput.setFont(new Font("SansSerif", Font.PLAIN, 16)); // Font

        // Increase the height of the search field
        searchField.setPreferredSize(new Dimension(100, 37)); // Adjust the width and height
        searchField.setFont(new Font("SansSerif", Font.PLAIN, 14)); // Font

        // Personalization of the send button
        sendButton.setText("Envoyer"); // Change the button text
        sendButton.setFont(new Font("SansSerif", Font.BOLD, 14)); // Change font size
        sendButton.setPreferredSize(new Dimension(100, 40)); // Change button size if necessary
    }

    // Custom renderer for JList
    class CustomCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            JLabel label = (JLabel) c;
            label.setIcon(new ImageIcon("path_to_icon.png")); // Replace with the actual path to the icon
            label.setHorizontalTextPosition(JLabel.RIGHT);
            label.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10)); // Margins around text
            if (isSelected) {
                label.setBackground(Color.LIGHT_GRAY);
            }
            return label;
        }
    }

    // PlaceholderTextField class for search field
    class PlaceholderTextField extends JTextField {
        private String placeholder;

        public PlaceholderTextField(String text, int columns) {
            super(text, columns);
        }

        public PlaceholderTextField(int columns) {
            super(columns);
        }

        public PlaceholderTextField(String text) {
            super(text);
        }

        public PlaceholderTextField() {
            super();
        }

        public String getPlaceholder() {
            return placeholder;
        }

        public void setPlaceholder(String placeholder) {
            this.placeholder = placeholder;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            if (placeholder == null || placeholder.length() == 0 || !getText().isEmpty()) {
                return;
            }

            Graphics2D g2 = (Graphics2D) g.create();
            g2.setColor(getDisabledTextColor());
            g2.setFont(getFont().deriveFont(Font.ITALIC));
            g2.drawString(placeholder, getInsets().left, g.getFontMetrics().getMaxAscent() + getInsets().top);
            g2.dispose();
        }

        @Override
        protected Document createDefaultModel() {
            return new PlaceholderDocument();
        }

        private class PlaceholderDocument extends PlainDocument {
            @Override
            public void insertString(int offs, String str, AttributeSet a) {
                if (str == null || str.isEmpty()) {
                    return;
                }
                try {
                    super.insertString(offs, str, a);
                } catch (Exception e) {
                    // do nothing
                }
            }
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

