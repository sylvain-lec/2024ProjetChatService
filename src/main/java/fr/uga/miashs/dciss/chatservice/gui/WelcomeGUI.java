/*
 * Copyright (c) 2024.  Jerome David. Univ. Grenoble Alpes.
 * This file is part of DcissChatService.
 *
 * DcissChatService is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * DcissChatService is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with Foobar. If not, see <https://www.gnu.org/licenses/>.
 */

package fr.uga.miashs.dciss.chatservice.gui;

import javax.swing.*;
import java.awt.*;

public class WelcomeGUI extends JFrame {
    private JPanel MainPanel;
    private JLabel titleLabel;
    private JLabel welcomeLabel;
    private JButton proceedButton;

    public WelcomeGUI() {
        // Set up the main panel
        MainPanel = new JPanel();
        MainPanel.setLayout(new BoxLayout(MainPanel, BoxLayout.Y_AXIS));

        // Set up the title label
        titleLabel = new JLabel("Welcome to Chat Service");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Set up the welcome message label
        welcomeLabel = new JLabel("<html><div style='text-align: center;'>This is a simple chat service.<br>Click the button below to proceed.</div></html>");
        welcomeLabel.setFont(new Font("Arial", Font.PLAIN, 18));
        welcomeLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Set up the proceed button
        proceedButton = new JButton("Proceed to Chat");
        proceedButton.setFont(new Font("Arial", Font.PLAIN, 18));
        proceedButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        proceedButton.addActionListener(e -> {
            // TODO: Add action to proceed to the chat application
        });

        // Add components to the main panel
        MainPanel.add(Box.createRigidArea(new Dimension(0, 50)));
        MainPanel.add(titleLabel);
        MainPanel.add(Box.createRigidArea(new Dimension(0, 20)));
        MainPanel.add(welcomeLabel);
        MainPanel.add(Box.createRigidArea(new Dimension(0, 20)));
        MainPanel.add(proceedButton);

        // Add the main panel to the frame
        add(MainPanel);
    }

    public static void main(String[] args) {
        JFrame frame = new WelcomeGUI();
        frame.setTitle("Welcome to Chat Service");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setLocationRelativeTo(null); // Center the frame
        frame.setVisible(true);
    }
}