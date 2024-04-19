package fr.uga.miashs.dciss.chatservice.gui;
import fr.uga.miashs.dciss.chatservice.client.ClientMsg;
import fr.uga.miashs.dciss.chatservice.client.ConnectionListener;
import fr.uga.miashs.dciss.chatservice.client.MessageListener;
import fr.uga.miashs.dciss.chatservice.common.Packet;
import fr.uga.miashs.dciss.chatservice.common.Packet;
import fr.uga.miashs.dciss.chatservice.server.ServerMsg;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import javax.swing.ImageIcon;
import javax.swing.text.*;

public class Chat implements MessageListener, ConnectionListener{

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
    private JButton deleteGroupButton;
    private JButton addMemberButton;
    private JButton removeMemberButton;
    private JButton changeUsernameButton;
    private JButton changePasswordButton;

    private ClientMsg clientMsg;
    private Socket s;


    public Chat() {
        // REGISTER to the server
        clientMsg = new ClientMsg("localhost", 1666);
        clientMsg.addMessageListener(this);
        clientMsg.addConnectionListener(this);
//        clientMsg.addMessageListener((MessageListener) this);
//        clientMsg.addConnectionListener((ConnectionListener) this);

        openFirstWindow();
    }
    private void openFirstWindow() {
        frame = new JFrame("Chat");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(300, 200);

        JButton registerButton = new JButton("Register");
        registerButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String password = JOptionPane.showInputDialog(frame, "Enter your password:");
                try {
                    clientMsg.startSession(password);
                    String username = JOptionPane.showInputDialog(frame, "Enter your username:");
                    clientMsg.setUsername(username);
                    System.out.println("User ID: " + clientMsg.getIdentifier() + ", Username: " + clientMsg.getUsername() + ", Password: " + password);
                    initializeUI();
                    customizeUIComponents();
                    initializeButtons();
                } catch (UnknownHostException ex) {
                    throw new RuntimeException(ex);
                }
            }
        });

        JButton loginButton = new JButton("Login");
        loginButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String userId = JOptionPane.showInputDialog(frame, "Enter your id:");
                String password = JOptionPane.showInputDialog(frame, "Enter your password:");

                try {
                    clientMsg.setIdentifier(Integer.parseInt(userId));
                    clientMsg.startSession(password);
                  //  clientMsg.askInfos();

                    System.out.println("User ID: " + clientMsg.getIdentifier() + ", Username: " + clientMsg.getUsername() + ", Password: " + password);
                    initializeUI();
                    customizeUIComponents();
                    initializeButtons();
                } catch (UnknownHostException ex) {
                    throw new RuntimeException(ex);
                }

            }
        });

        JPanel panel = new JPanel();
        panel.add(registerButton);
        panel.add(loginButton);

        frame.getContentPane().add(panel, BorderLayout.CENTER);
        frame.setVisible(true);
    }


    private void initializeButtons() {
        addContactButton = new JButton("Ajouter Contact");
        createGroupButton = new JButton("Créer Groupe");
        deleteGroupButton = new JButton("Supprimer Groupe");
        addMemberButton = new JButton("Ajouter Membre");
        removeMemberButton = new JButton("Supprimer Membre");
        changeUsernameButton = new JButton("Changer Nom");
        changePasswordButton = new JButton("Changer Password");

        addContactButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String contactIdString = JOptionPane.showInputDialog(frame, "Enter contact's ID:");
                String contactName = JOptionPane.showInputDialog(frame, "Enter contact's name:");
                if (contactIdString != null && !contactIdString.trim().isEmpty() && contactName != null && !contactName.trim().isEmpty()) {
                    int contactId = Integer.parseInt(contactIdString);
                    //clientMsg.addContact(contactId, contactName);
                    System.out.println("Le contact \"" + contactName + "\" a été ajouté avec succès.");
                    JOptionPane.showMessageDialog(frame, "Le contact \"" + contactName + "\" a été ajouté avec succès.", "Succès", JOptionPane.INFORMATION_MESSAGE);


                }
            }
        });

        createGroupButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Create a list to store the members
                ArrayList<Integer> members = new ArrayList<>();

                // Continuously show the input dialog until the user presses cancel
                while (true) {
                    String memberString = JOptionPane.showInputDialog(frame, "Ajoutez un membre (ou appuyez sur Annuler pour terminer):");

                    // If the user pressed cancel, memberString will be null
                    if (memberString == null || memberString.trim().isEmpty()) {
                        break;
                    }
                    // Convert the input to an integer and add it to the list
                    try {
                        int memberId = Integer.parseInt(memberString);
                        members.add(memberId);
                    } catch (NumberFormatException ex) {
                        JOptionPane.showMessageDialog(frame, "Invalid input. Please enter a number.", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
                clientMsg.creationGroupe(members);
                JOptionPane.showMessageDialog(frame, "Le groupe a été créé avec succès.", "Succès", JOptionPane.INFORMATION_MESSAGE);


                // Create the group with the list of members
//                if (!members.isEmpty()) {
//                    clientMsg.creationGroupe(members);
//                    JOptionPane.showMessageDialog(frame, "Le groupe a été créé avec succès.", "Succès", JOptionPane.INFORMATION_MESSAGE);
//                }
//
//                String memberString = JOptionPane.showInputDialog(frame, "Ajoutez un membre :");
//                if (groupIdString != null && !groupIdString.trim().isEmpty()) {
//
//                    clientMsg.creationGroupe();
//                    JOptionPane.showMessageDialog(frame, "Le groupe \"" + groupIdString + "\" a été créé avec succès.", "Succès", JOptionPane.INFORMATION_MESSAGE);
//
//                }
            }

        });
        deleteGroupButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String groupIdString = JOptionPane.showInputDialog(frame, "Entrez l'ID du groupe:");
                if (groupIdString != null && !groupIdString.trim().isEmpty()) {
                    int groupId = Integer.parseInt(groupIdString);
                    clientMsg.supprimerGroupe(groupId);
                    JOptionPane.showMessageDialog(frame, "Le groupe \"" + groupIdString + "\" a été supprimé avec succès.", "Succès", JOptionPane.INFORMATION_MESSAGE);

                }
            }
        });
        addMemberButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String groupIdString = JOptionPane.showInputDialog(frame, "Enter the group ID:");
                String userIdString = JOptionPane.showInputDialog(frame, "Enter the user ID:");
                if (groupIdString != null && !groupIdString.trim().isEmpty() && userIdString != null && !userIdString.trim().isEmpty()) {
                    try {
                        int groupId = Integer.parseInt(groupIdString);
                        int userId = Integer.parseInt(userIdString);
                        clientMsg.addMember(groupId, userId);
                        JOptionPane.showMessageDialog(frame, "Le groupe \"" + groupId + "\" a ajouté" +userId+"avec succès.", "Succès", JOptionPane.INFORMATION_MESSAGE);

                    } catch (NumberFormatException ex) {
                        JOptionPane.showMessageDialog(frame, "Invalid input. Please enter a valid integer for group ID and user ID.");
                    }
                }
            }
        });

        removeMemberButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String groupIdString = JOptionPane.showInputDialog(frame, "Entrez l'ID du groupe:");
                String userIdString = JOptionPane.showInputDialog(frame, "Entrez l'ID du membre:");
                if (groupIdString != null && !groupIdString.trim().isEmpty() && userIdString != null && !userIdString.trim().isEmpty()) {
                    try {
                        int groupId = Integer.parseInt(groupIdString);
                        int userId = Integer.parseInt(userIdString);
                        clientMsg.removeMember(groupId, userId);
                    } catch (NumberFormatException ex) {
                        JOptionPane.showMessageDialog(frame, "Invalid input. Please enter a valid integer for group ID and user ID.");
                    }
                }
            }
        });
        changeUsernameButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String newUsername = JOptionPane.showInputDialog(frame, "Entrez le nouveau nom d'utilisateur:");
                if (newUsername != null && !newUsername.trim().isEmpty()) {
                    clientMsg.setUsername(newUsername);
                }
            }
        });
        changePasswordButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String newPassword = JOptionPane.showInputDialog(frame, "Entrez le nouveau mot de passe:");
                if (newPassword != null && !newPassword.trim().isEmpty()) {
                    clientMsg.updatePassword();

                }
            }
        });


        // Panel with a BoxLayout for the buttons
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.PAGE_AXIS));

        // Add buttons to the panel
        buttonPanel.add(addContactButton);
        buttonPanel.add(Box.createRigidArea(new Dimension(0, 5))); // This adds space between buttons
        buttonPanel.add(createGroupButton);
        buttonPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        buttonPanel.add(deleteGroupButton);
        buttonPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        buttonPanel.add(addMemberButton);
        buttonPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        buttonPanel.add(removeMemberButton);
        buttonPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        buttonPanel.add(changeUsernameButton);
        buttonPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        buttonPanel.add(changePasswordButton);
        buttonPanel.add(Box.createRigidArea(new Dimension(0, 5)));

        buttonPanel.add(Box.createVerticalGlue());

        // Set alignment for all components within the panel
        for (Component comp : buttonPanel.getComponents()) {
            ((JComponent) comp).setAlignmentX(Component.LEFT_ALIGNMENT);
        }

        leftPanel.add(buttonPanel, BorderLayout.NORTH);

        frame.validate();
        frame.repaint();    }
   /* private void createGroup(String groupName) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(bos);
        dos.writeUTF(groupName);
        clientMsg.sendPacket(1, bos.toByteArray());
        dos.close();
        bos.close();
        contactListModel.addElement(groupName);
    }*/
    /*private void addContactToUser() {
        String contactIdString = JOptionPane.showInputDialog(frame, "Enter contact's ID:");
        String contactName = JOptionPane.showInputDialog(frame, "Enter contact's name:");
        if (contactIdString != null && !contactIdString.trim().isEmpty() && contactName != null && !contactName.trim().isEmpty()) {
            int contactId = Integer.parseInt(contactIdString);
            try {
                clientMsg.addContact(contactId, contactName);
                JOptionPane.showMessageDialog(frame, "Contact added successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(frame, "Error while adding contact.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void changeUserUsername() {
        String newUsername = JOptionPane.showInputDialog(frame, "Enter your new username:");
        if (newUsername != null && !newUsername.trim().isEmpty()) {
            try {
                clientMsg.changeUsername(newUsername);
                JOptionPane.showMessageDialog(frame, "Username changed successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(frame, "Error while changing username.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void changeUserPassword() {
        String newPassword = JOptionPane.showInputDialog(frame, "Enter your new password:");
        if (newPassword != null && !newPassword.trim().isEmpty()) {
            try {
                clientMsg.changePassword(clientMsg.getPassword(), newPassword);
                JOptionPane.showMessageDialog(frame, "Password changed successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(frame, "Error while changing password.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }*/



    private void initializeUI() {
        frame = new JFrame("Chat");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);

        // Create the top panel
        topPanel = new JPanel(new BorderLayout());
        leftTopPanel = new JPanel();
        searchField = new PlaceholderTextField(15);
        //searchField.setFocusable(false);
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
                        showUserProfile();
                        break;
                    case "Log In":
                        try {
                            showLoginDialog();
                        } catch (UnknownHostException ex) {
                            throw new RuntimeException(ex);
                        }
                        break;
                    case "Log Out":
                        performLogout();
                        frame.removeAll();
                        break;
                }
            }

            private void showUserProfile() {
                String userInfo = clientMsg.getClientInfo(); // Implement getClientInfo to return user details
                JOptionPane.showMessageDialog(frame, userInfo, "Profile Information", JOptionPane.INFORMATION_MESSAGE);

            }
            private void showLoginDialog() throws UnknownHostException {
                JTextField idField = new JTextField(5);
                JPasswordField passwordField = new JPasswordField(5);

                JPanel myPanel = new JPanel();
                myPanel.add(new JLabel("id:"));
                myPanel.add(idField);
                int id = Integer.parseInt(idField.getText());
                clientMsg.setIdentifier(id) ;
                myPanel.add(Box.createHorizontalStrut(15)); // a spacer
                myPanel.add(new JLabel("Password:"));
                myPanel.add(passwordField);

                int result = JOptionPane.showConfirmDialog(null, myPanel,
                        "Votre id et Password", JOptionPane.OK_CANCEL_OPTION);
                if (result == JOptionPane.OK_OPTION) {
                    clientMsg.startSession(new String(passwordField.getPassword()));
                }
            }


            private void performLogout() {
                clientMsg.endSession();
                frame.removeAll();
                JOptionPane.showMessageDialog(frame, "Vous avez Logout.", "Logout Succèss", JOptionPane.INFORMATION_MESSAGE);
                //remove all
//                clientMsg = new ClientMsg("localhost", 1666);
//////        clientMsg.addMessageListener((MessageListener) this);
////        clientMsg.addConnectionListener((ConnectionListener) this);
//                String password = JOptionPane.showInputDialog(frame, "Enter your password:");
//                try {
//                    clientMsg.startSession(password);
//                    //ask the client to choose a username
//                    String username = JOptionPane.showInputDialog(frame, "Enter your username:");
//                    clientMsg.setUsername(username);
//                    //print somewhere the userid, username and password
//                    System.out.println("User ID: " + clientMsg.getIdentifier() + ", Username: " + clientMsg.getUsername() + ", Password: " + password);
//                } catch (UnknownHostException e) {
//                    throw new RuntimeException(e);
//                }
//
//                initializeUI();
//                customizeUIComponents();
//                initializeButtons();

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
                String message = messageInput.getText().trim();
                // display dialog pour demander le destinataire id
                String destIdString = JOptionPane.showInputDialog(frame, "Entrez l'ID du destinataire:");
                int destId = Integer.parseInt(destIdString);
                if (!message.isEmpty()) {
                    clientMsg.sendPacket(destId, message.getBytes(StandardCharsets.UTF_8));
                    chatArea.append("Vous: " + message + "\n");
                    messageInput.setText(""); // Clear input after sending

                }
            }
        });
        inputPanel.add(sendButton, BorderLayout.EAST);


        // Add Enter key listener to send messages

        messageInput.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                clientMsg.sendPacket(clientMsg.getIdentifier(), messageInput.getText().getBytes());
            }
        });

        rightPanel.add(inputPanel, BorderLayout.SOUTH);

        // Add the two panels to the split pane
        splitPane.setLeftComponent(leftPanel);
        splitPane.setRightComponent(rightPanel);

        frame.add(splitPane);
        frame.setVisible(true);
    }

    /*private void sendMessage() {
        String message = messageInput.getText().trim();
        if (!message.isEmpty()) {
            chatArea.append("Vous: " + message + "\n");  // Display the message in the chat area
            messageInput.setText("");  // Clear the text input field
            this.clientMsg = new ClientMsg("localhost", 1666);
            try {
                clientMsg.startSession();
            } catch (UnknownHostException e) {
                e.printStackTrace();
                return;
            }
            int dest = 0;
            clientMsg.sendPacket(dest, message.getBytes());
            initializeUI();
            customizeUIComponents();
            initializeButtons();

        }
    }*/

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

    @Override
    public void connectionEvent(boolean active) {

    }

    @Override
    public void messageReceived(Packet p) {
        try {
        DataInputStream dis = new DataInputStream(s.getInputStream());

            while (s != null && !s.isClosed()) {
                int sender = dis.readInt();
                int dest = dis.readInt();
                int length = dis.readInt();
                byte[] data = new byte[length];
                dis.readFully(data);

                Packet packet = new Packet(sender, dest, data); // Create a packet to receive an image if there is one

                if (sender == ServerMsg.SERVER_CLIENTID && dest == clientMsg.getIdentifier()) {
                    ByteBuffer buffer = ByteBuffer.wrap(data);
                    // Suppose que le serveur envoie un byte pour définir le type de réponse.
                    byte responseType = buffer.get();

                    if (responseType == 1) { //création de groupe
                        int groupId = buffer.getInt();
                        int lengthMsg = buffer.getInt();
                        byte[] msgBytes = new byte[lengthMsg];
                        buffer.get(msgBytes);
                        String msg = new String(msgBytes, StandardCharsets.UTF_8);
                        System.out.println(msg);

                    }
                    else if (responseType == 2 || responseType == 3 || responseType == 4) { //handle group deletion, whether it worked or not
                        int lengthMsg = buffer.getInt();
                        byte[] msgBytes = new byte[lengthMsg];
                        buffer.get(msgBytes);
                        String msg = new String(msgBytes, StandardCharsets.UTF_8);
                        System.out.println(msg);
                    }

                    else if (responseType == 9) { //info retrieval upon authentication
                        int usernameLength = buffer.getInt();
                        byte[] usernameBytes = new byte[usernameLength];
                        buffer.get(usernameBytes);
                        String username = new String(usernameBytes, StandardCharsets.UTF_8); //retrieve the username
                    //    this.username = username; //set the username

                        int passwordLength = buffer.getInt();
                        byte[] passwordBytes = new byte[passwordLength];
                        buffer.get(passwordBytes);
                        String password = new String(passwordBytes, StandardCharsets.UTF_8); //retrieve the password
                     //   this.password = password; //set the password
                    }
                } else {
                 //   notifyMessageListeners(new Packet(sender, dest, data));
                }
            }
        } catch (IOException e) {
            // En cas d'erreur, fermer la connexion
            e.printStackTrace();
        }
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
