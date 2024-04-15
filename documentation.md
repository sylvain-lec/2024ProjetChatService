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

   **Environment Requirements**: List of necessary software, tools, and libraries needed to run the project.

   **Installation Instructions**: Step-by-step guide to setting up the project in a development environment, including any dependencies.

## 3. Feature Documentation - TODO

   **For each feature** developed during the week, include:
   * **Feature Overview**: Description of what the feature does and why it’s important.  
   * **Technical Implementation**: Detailed explanation of how the feature was implemented, including any significant classes, methods, and algorithms used.  
   * **Configuration**: Any configurations needed to customize or optimize the feature.

## 4. User Guide - TODO

   **How to Use**: Instructions on how to use the application and its features, potentially including screenshots or diagrams for clarity.  
   **Troubleshooting**: Common issues that may arise and their solutions.

## 5. Testing - TODO

   **Testing Strategies**: Overview of the testing approach, including types of tests used (unit, integration, system).  
   **Test Cases**: Description of important test cases and expected outcomes for each feature.  
   **Bugs and Issues**: Known bugs and their current status, along with any workarounds.