package fr.uga.miashs.dciss.chatservice.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class Chat {

    // Main frame components
    private JFrame frame;
    private JTextArea messageArea;
    private JTextField inputField;
    private JButton sendButton;
    private JLabel statusBar;

    public Chat() {
        initializeUI();
    }

    private void initializeUI() {
        frame = new JFrame("Chat Service");
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.setSize(600, 400);
        frame.setLayout(new BorderLayout());

        // Confirm on close
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (JOptionPane.showConfirmDialog(frame,
                        "Are you sure you want to close the application?", "Close Window?",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION) {
                    System.exit(0);
                }
            }
        });

        // Text area for messages
        messageArea = new JTextArea();
        messageArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(messageArea);
        frame.add(scrollPane, BorderLayout.CENTER);

        // Status bar
        statusBar = new JLabel("Not Connected");
        frame.add(statusBar, BorderLayout.SOUTH);

        // Input panel setup
        JPanel inputPanel = new JPanel(new BorderLayout());
        inputField = new JTextField();
        sendButton = new JButton("Send");
        inputPanel.add(inputField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);
        frame.add(inputPanel, BorderLayout.SOUTH);

        // Setup send action
        ActionListener sendListener = e -> sendMessage();
        sendButton.addActionListener(sendListener);
        inputField.addActionListener(sendListener);

        // Show frame
        frame.setVisible(true);
    }

    private void sendMessage() {
        String message = inputField.getText().trim();
        if (!message.isEmpty()) {
            messageArea.append("You: " + message + "\n");
            inputField.setText("");
            // Here you would normally also send the message over the network
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Chat::new);
    }
}
