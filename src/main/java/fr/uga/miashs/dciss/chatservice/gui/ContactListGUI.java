package fr.uga.miashs.dciss.chatservice.gui;

import javax.swing.*;
import java.awt.*;
import java.util.List;


public class ContactListGUI {

    private JFrame frame;
    private JList<Contact> contactJList;
    private DefaultListModel<Contact> contactListModel;
    private JTextField searchField;
    private ContactDAO contactDAO;

    // Assuming Contact is a custom class that holds information about a contact
    public static class Contact {
        private int contactId;
        private String name;
        private Icon icon;

        public Contact(int contactId, String name, Icon icon) {
            this.contactId = contactId;
            this.name = name;
            this.icon = icon;
        }

        // getters...
        public int getContactId() {
            return contactId;
        }

        public String getName() {
            return name;
        }

        public Icon getIcon() {
            return icon;
        }
    }

    public ContactListGUI() {
        initializeUI();
        contactDAO = new ContactDAO(); // Initialize ContactDAO
        populateContactList(); // Populate the contact list when the GUI is initialized
    }

    private void initializeUI() {
        frame = new JFrame("Contact List");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(300, 500);

        // Create the contact list panel
        JPanel contactListPanel = new JPanel(new BorderLayout());
        contactListPanel.setPreferredSize(new Dimension(200, 500));

        // Create the search bar
        searchField = new JTextField();
        searchField.addActionListener(e -> {
            // Implement search functionality
        });

        // Create the contact list model
        contactListModel = new DefaultListModel<>();

        // Create the JList with a custom cell renderer
        contactJList = new JList<>(contactListModel);
        contactJList.setCellRenderer(new ContactCellRenderer());

        // Add the components to the frame
        frame.add(searchField, BorderLayout.NORTH);
        frame.add(new JScrollPane(contactJList), BorderLayout.CENTER);

        // Display the frame
        frame.setVisible(true);
    }

    // Method to populate the contact list from the database
    private void populateContactList() {
        List<Contact> contacts = contactDAO.getAllContacts();
        for (Contact contact : contacts) {
            contactListModel.addElement(contact);
        }
    }

    // Custom cell renderer for displaying contacts
    private class ContactCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof Contact) {
                Contact contact = (Contact) value;
                setText(contact.getName());
                setIcon(contact.getIcon());
            }
            return this;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ContactListGUI::new);
    }
}
