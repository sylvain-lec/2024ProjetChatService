package fr.uga.miashs.dciss.chatservice.gui;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ContactDAO {
    // Chargement du pilote JDBC SQLite dans un bloc statique
    static {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    //path to my database
     private static final String URL = "jdbc:sqlite:./database.db";


    // Méthode pour récupérer tous les contacts depuis la base de données
    public List<ContactListGUI.Contact> getAllContacts() {
        List<ContactListGUI.Contact> contacts = new ArrayList<>();

        try (Connection connection = DriverManager.getConnection(URL)) {
            PreparedStatement statement = connection.prepareStatement("SELECT * FROM contacts");
            ResultSet resultSet = statement.executeQuery();

            while (resultSet.next()) {
                int contactId = resultSet.getInt("contactId");
                String name = resultSet.getString("name");
                // Créez un objet Contact et ajoutez-le à la liste
                contacts.add(new ContactListGUI.Contact(contactId, name));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return contacts;
    }

    public void createContactsTable() {
        try (Connection connection = DriverManager.getConnection(URL)) {
            PreparedStatement statement = connection.prepareStatement(
                    "CREATE TABLE IF NOT EXISTS contacts (" +
                            "contactId INTEGER PRIMARY KEY," +
                            "name TEXT" +
                            ")"
            );
            statement.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Méthode pour insérer un nouveau contact dans la base de données
    public boolean insertContact(ContactListGUI.Contact contact) {
        try (Connection connection = DriverManager.getConnection(URL)) {
            PreparedStatement statement = connection.prepareStatement("INSERT INTO contacts (contactId, name) VALUES (?, ?)");
            statement.setInt(1, contact.getContactId());
            statement.setString(2, contact.getName());
            int rowsInserted = statement.executeUpdate();
            return rowsInserted > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}
