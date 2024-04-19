## 1. Overview

   The DCISS Chat Service is a messaging application designed to emulate core functionalities of popular messaging apps like WhatsApp. 
   It allows users to send and receive messages, both in individual and group settings, thereby supporting real-time communication across a network.  
   The project is built on Java and leverages client-server architecture to handle data transmission, user management, and message processing. 
   The goal is to provide a robust, scalable, and secure platform for text-based communication, with potential extensions for media sharing and enhanced user interaction.
   
   The system is divided into several main components, each responsible for different aspects of the application:  

   * **Client**: Handles user interaction, sends messages to the server, and receives updates.  
   Implemented in files like ClientMsg.java, which manages the client-side messaging logic.  

  * **Server**: Manages all clients, processes incoming packets, and routes them to the appropriate recipients.  
  The server functionality is encapsulated in files such as ServerMsg.java and ServerPacketProcessor.java, which together handle client connections, message dispatch, and basic server operations.  

  * **Packet**: Serves as the basic data structure for messages sent across the network, defined in Packet.java.  
  It includes essential data such as source and destination IDs, as well as the message content itself.

  * **Packet Processor**: A component that processes the packets based on their type and content.  
  This logic is split across PacketProcessor.java for generic processing and ServerPacketProcessor.java for server-specific handling.

## 2. Setup and Installation - TODO

**Environment Requirements**: Java and Maven are required to run the project.

**Installation Instructions**: Clone the repository, navigate to the project directory, and run `mvn install` to install the necessary dependencies.

## 3. Feature Documentation

**Login Feature**:
* **Feature Overview**: This feature allows users to log in to the DCISS Chat Service by providing a username and password.
* **Technical Implementation**: The login functionality is implemented in the `login` method in the `ClientMsg.java` file. It sends a login packet to the server, which verifies the user's credentials and responds with a success or failure message.
* **Error Handling**: If the login fails, the client displays an error message to the user.

**Group Chat Feature**:
* **Feature Overview**: This feature allows users to create and participate in group chats with multiple participants.
* **Technical Implementation**: The group chat functionality is implemented in the `sendPacket` method in the `ClientMsg.java` file. It sends a packet of data to the server, which then routes it to all members of the group.

**Contact List Feature**:
* **Feature Overview**: This feature displays a list of contacts that the user can interact with, including sending messages and viewing their status.
* **Technical Implementation**: The contact list is populated by the server and sent to the client when the user logs in. It is displayed in the GUI using a list or table view, with options for selecting contacts and initiating conversations.

**Send Message Feature**:
* **Feature Overview**: This feature allows the client to send a message to another client or a group of clients.
* **Technical Implementation**: The feature is implemented in the `sendPacket` method in the `ClientMsg.java` file. It sends a packet of data to the server, which then routes it to the appropriate recipients.

**Send File Feature**:
* **Feature Overview**: This feature allows the client to send a file to another client or a group of clients.
* **Technical Implementation**: The feature is implemented in the `sendFile` method in the `ClientMsg.java` file. It sends a file as a packet of data to the server, which then routes it to the appropriate recipients. The file is read into a byte array, which is then sent as part of the packet. On the receiving end, the byte array is converted back into a file.

**Graphical User Interface (GUI) Feature**:
* **Feature Overview**: This feature provides a user-friendly interface for interacting with the DCISS Chat Service. It includes windows for logging in, sending messages, viewing contact lists, and more.
* **Technical Implementation**: The GUI is implemented using Java's Swing . It includes classes for each window or panel in the application, such as `LoginWindow.java`, `ChatWindow.java`, `ContactListPanel.java`, etc. Each class contains methods for initializing the GUI components, handling user input, and updating the display.

## 4. User Guide - TODO

   **How to Use**: Instructions on how to use the application and its features, potentially including screenshots or diagrams for clarity.  
   **Troubleshooting**: Common issues that may arise and their solutions.

## 5. Testing - TODO

   **Testing Strategies**: Overview of the testing approach, including types of tests used (unit, integration, system).  
   **Test Cases**: Description of important test cases and expected outcomes for each feature.  
   **Bugs and Issues**: Known bugs and their current status, along with any workarounds.