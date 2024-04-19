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

package fr.uga.miashs.dciss.chatservice.client;

import java.awt.image.BufferedImage;
import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import fr.uga.miashs.dciss.chatservice.common.Packet;
import fr.uga.miashs.dciss.chatservice.server.ServerMsg;

import javax.imageio.ImageIO;

/**
 * Manages the connection to a ServerMsg. Method startSession() is used to
 * establish the connection. Then messages can be send by a call to sendPacket.
 * The reception is done asynchronously (internally by the method receiveLoop())
 * and the reception of a message is notified to MessagesListeners. To register
 * a MessageListener, the method addMessageListener has to be called. Session
 * are closed thanks to the method closeSession().
 */
public class ClientMsg {

	private String serverAddress;
	private int serverPort;

	private Socket s;
	private DataOutputStream dos;
	private DataInputStream dis;

	private int identifier;
	private String username;
	private String password;
	private volatile boolean isAuthenticated = false;


	private List<MessageListener> mListeners;
	private List<ConnectionListener> cListeners;

	/**
	 * Create a client with an existing id, that will connect to the server at the
	 * given address and port
	 *
	 * @param id      The client id
	 * @param address The server address or hostname
	 * @param port    The port number
	 */
	public ClientMsg(int id, String address, int port, String username, String password) {
		if (id < 0)
			throw new IllegalArgumentException("id must not be less than 0");
		if (port <= 0)
			throw new IllegalArgumentException("Server port must be greater than 0");
		serverAddress = address;
		serverPort = port;
		identifier = id;
		mListeners = new ArrayList<>();
		cListeners = new ArrayList<>();
		this.username = username;
		this.password = password;
	}

	/**
	 * Create a client without id, the server will provide an id during the the
	 * session start
	 *
	 * @param address The server address or hostname
	 * @param port    The port number
	 */
	public ClientMsg(String address, int port) {
		this(0, address, port, "defaultUsername", "password");
	}

	/**
	 * Register a MessageListener to the client. It will be notified each time a
	 * message is received.
	 *
	 * @param l
	 */
	public void addMessageListener(MessageListener l) {
		if (l != null)
			mListeners.add(l);
	}
	protected void notifyMessageListeners(Packet p) {
		mListeners.forEach(x -> x.messageReceived(p));
	}

	/**
	 * Register a ConnectionListener to the client. It will be notified if the connection  start or ends.
	 *
	 * @param l
	 */
	public void addConnectionListener(ConnectionListener l) {
		if (l != null)
			cListeners.add(l);
	}
	protected void notifyConnectionListeners(boolean active) {
		cListeners.forEach(x -> x.connectionEvent(active));
	}


	public int getIdentifier() {
		return identifier;
	}
	public String getUsername() { return username; }

	public void setIdentifier(int identifier) {
		this.identifier = identifier;
	}

	/**
	 * sets username client-side, and sends a packet to the server to update the username server-side.
	 * @param username
	 */
	public void setUsername(String username) {
		this.username = username;

		//send packet to the server; the server will update the username.
		//1byte for the type (4), 4bytes (an int) for the length of the username, then the username
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(bos);
		try {
			dos.writeByte(5);
			dos.writeInt(username.length());
			dos.write(username.getBytes(StandardCharsets.UTF_8));
			dos.flush();
			sendPacket(0, bos.toByteArray());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Sets password client side, and sends a packet to the server to update the password server-side.
	 * @param password
	 */
	public void setPassword(String password) {
		this.password = password;

        //send packet to the server; the server will update the password.
        //1byte for the type (7), 4bytes (an int) for the length of the password, then the password
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(bos);
        try {
            dos.writeByte(7);
            dos.writeInt(password.length());
            dos.write(password.getBytes(StandardCharsets.UTF_8));
            dos.flush();
            sendPacket(0, bos.toByteArray());
        } catch (IOException e) {
            e.printStackTrace();
        }
	}

	public String getPassword() {
		return password;
	}



	/**
	 * Method to be called to establish the connection.
	 *
	 * @throws UnknownHostException
	 * @throws IOException
	 */
	public void startSession(String password) throws UnknownHostException {
		if (s == null || s.isClosed()) {
			try {
				s = new Socket(serverAddress, serverPort);
				dos = new DataOutputStream(s.getOutputStream());
				dis = new DataInputStream(s.getInputStream());
				dos.writeInt(identifier);
				dos.writeUTF(password);
				dos.flush();
				if (identifier == 0) {
					identifier = dis.readInt();
				}
				// start the receive loop
				new Thread(() -> {
                    try {
                        receiveLoop();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }).start();
				notifyConnectionListeners(true);

			} catch (IOException e) {
				e.printStackTrace();
				// error, close session
				closeSession();
			}
		}
	}

	/**
	 * Send a packet to the specified destination (etiher a userId or groupId)
	 *
	 * @param destId the destinatiion id
	 * @param data   the data to be sent
	 */
	public void sendPacket(int destId, byte[] data) {
		try {
			synchronized (dos) {
				dos.writeInt(destId);
				dos.writeInt(data.length);
				dos.write(data);
				dos.flush();
			}
		} catch (IOException e) {
			// error, connection closed
			closeSession();
		}
    }

	/**
	 * Method to send files with their name and extension
	 */
	public void sendFile(int destId, Path filePath, String filename) {
		try {
			byte[] data = Files.readAllBytes(filePath);
			System.out.println("File read successfully, size: " + data.length + " bytes");
			String fileExtension = getFileExtension(filePath);
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			DataOutputStream dos = new DataOutputStream(bos);
			dos.writeByte(12); // send file
			//dos.writeInt(filename.length()); // send size of filename
			dos.writeUTF(filename);
			//dos.writeInt(fileExtension.length()); // send size of file extension
			dos.writeUTF(fileExtension);
			dos.writeInt(data.length); // send size of file content
			dos.write(data);
			dos.flush();
			sendPacket(destId, bos.toByteArray());
			System.out.println("File sent successfully");
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Method to get the file extension
	 */
	private String getFileExtension(Path filePath) {
		String fileName = filePath.getFileName().toString();
		int dotIndex = fileName.lastIndexOf('.');
		return (dotIndex == -1) ? "" : fileName.substring(dotIndex + 1);
	}

	/**
	 * Method to handle file packets
	 */
	private void handleFilePacket(byte[] data/*, String filePath*/) throws IOException {
		DataInputStream dis = new DataInputStream(new ByteArrayInputStream(data));

		byte type = dis.readByte();
		if (type != 12) {
			throw new IllegalArgumentException("Invalid file packet type: " + type);
		}
		String filename = dis.readUTF();
		String fileExtension = dis.readUTF();
		int fileContentLength = dis.readInt(); // read the size of the file content for syso

		//if (filePath == null) {
			// VERSION avec envoi du fichier dans le dossier courant
			Files.copy(dis, Paths.get(filename)); // write the file content to the file
			System.out.println("File received successfully in current folder, size: " + fileContentLength + " bytes");
		/*}
		else {
			// VERSION avec envoi du fichier dans un dossier spécifique
			Path path = Path.of(filePath + "/" + filename + "." + fileExtension);
			Files.copy(dis, path); // write the file content to the file
			System.out.println("File received successfully in folder " + filePath +  ", size: " + fileContentLength + " bytes");
		}*/
	}

		/**
         * Start the receive loop. Has to be called only once.
         */
	private void receiveLoop() throws IOException {
		try {
			while (s != null && !s.isClosed()) {
				int sender = dis.readInt();
				int dest = dis.readInt();
				int length = dis.readInt();
				byte[] data = new byte[length];
				dis.readFully(data);

				if (sender == ServerMsg.SERVER_CLIENTID && dest == this.identifier) {
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

					} else if (responseType == 2 || responseType == 3 || responseType == 4) { //handle group deletion, whether it worked or not
						int lengthMsg = buffer.getInt();
						byte[] msgBytes = new byte[lengthMsg];
						buffer.get(msgBytes);
						String msg = new String(msgBytes, StandardCharsets.UTF_8);
						System.out.println(msg);
					} else if (responseType == 9) { //info retrieval upon authentication
						int usernameLength = buffer.getInt();
						byte[] usernameBytes = new byte[usernameLength];
						buffer.get(usernameBytes);
						String username = new String(usernameBytes, StandardCharsets.UTF_8); //retrieve the username
						this.username = username; //set the username

						int passwordLength = buffer.getInt();
						byte[] passwordBytes = new byte[passwordLength];
						buffer.get(passwordBytes);
						String password = new String(passwordBytes, StandardCharsets.UTF_8); //retrieve the password
						this.password = password; //set the password
					}
					// if packet comes from another user
					// if it's a file
				} else if (data[0] == 12) {
					// prompt the user for a file path
					/*System.out.println("Someone wants to send you a file. Enter the path where you want to save it: ");
					Scanner scanner = new Scanner(System.in);
					String filePath = scanner.nextLine();
					handleFilePacket(data, filePath);*/
					handleFilePacket(data);
					// if it's a message
				} else {
					notifyMessageListeners(new Packet(sender, dest, data));
				}
			}
		} catch (IOException e) {
			// En cas d'erreur, fermer la connexion
			e.printStackTrace();
		}
		closeSession();
	}

	public void closeSession() {
		try {
			if (s != null)
				s.close();
		} catch (IOException e) {
		}
		s = null;
		notifyConnectionListeners(false);
	}

	public static void main(String[] args) throws UnknownHostException, IOException, InterruptedException {
		ClientMsg c = new ClientMsg("localhost", 1666);

		// add a dummy listener that print the content of message as a string

		c.addMessageListener(p -> {
			System.out.println(p.srcId + " says to " + p.destId + ": " + new String(p.data));
		});

		// add a connection listener that exit application when connection closed
		c.addConnectionListener(active ->  {if (!active) System.exit(0);});

		Scanner sc = new Scanner(System.in);

		/* ----------------- AUTHENTIFICATION ------------------------- */
		String rep = "b" ;
		c.isAuthenticated = false ;

		while (!c.isAuthenticated && !rep.equalsIgnoreCase("R")) {
			//ask to register or authenticate
			System.out.println("Do you want to authenticate or register as a new member? (A/R)");
			rep = sc.nextLine();

			//AUTHENTIFICATION :
			if (rep.equalsIgnoreCase("A")) {
				System.out.println("what is your id ?");
				int id = Integer.parseInt(sc.nextLine());
				c.identifier = id;
				System.out.println("Enter your password: ");
				String password = sc.nextLine();
				c.startSession(password); //prendre en paramètre un mot de passe

				//send packet to retrieve username and password from server
				c.askInfos();
				c.isAuthenticated = true ;

			//NEW USER : registers with an id given by the server. username and password chosen by the user
			} else if (rep.equalsIgnoreCase("R")) {
				System.out.println("Enter your password: ");
				String password = sc.nextLine();
				c.startSession(password);
				System.out.println("Enter your username: ");
				String username = sc.nextLine();
				c.setUsername(username);
				c.password = password;

				System.out.println("you are now registered as " + c.getUsername() + " with id " + c.getIdentifier());
				c.isAuthenticated = true ;
			}
		}

		//wait for 0.5s
		Thread.sleep(500);
		//now, either the user registered, or the user is authenticated
		System.out.println("Hello "+ c.getUsername() + "!");

		String lu = null;
		while (!"\\quit".equals(lu)) {
			try {
				System.out.println("\n" + c.getUsername()+ ", que souhaitez-vous faire? \n0 : envoyer un message\n1 : envoyer un fichier\n2 : créer un groupe\n3 : supprimer un groupe\n4 : ajouter un membre à un groupe\n5 : supprimer un membre d'un groupe\n6 : changer de nom\n7 : changer de mot de passe\n8 : Ajouter un contact\n9 : Afficher la liste des contacts\n");
				int code = Integer.parseInt(sc.nextLine());

				if (code == 0) { //envoyer un msg
					System.out.println("\nA qui voulez vous écrire ? ");
					int dest = Integer.parseInt(sc.nextLine());
					System.out.println("\nVotre message ? ");
					lu = sc.nextLine();
					c.sendPacket(dest, lu.getBytes());

				} else if (code == 1) { //envoyer un fichier
					System.out.println("\nA qui voulez vous envoyer un fichier ? ");
					int dest = Integer.parseInt(sc.nextLine());
					System.out.println("\nChemin du fichier ? ");
					String path = sc.nextLine(); // Use nextLine() to read the file path
					Path filePath = Path.of(path);
					// Path is root of user
					// Path filePath = Path.of(System.getProperty("user.dir"));
					String filename = filePath.getFileName().toString();
					c.sendFile(dest, filePath, filename);

				} else if (code == 2) { //creer un groupe
					List<Integer> members = new ArrayList<>();
					int member = 1;
					// list of members
					while (member != 0) {
						System.out.println("add a member : (type 0 to exit)");
						member = Integer.parseInt(sc.nextLine());
						members.add(member);
					}
					c.creationGroupe(members);
				}

				else if (code == 3) { //supprimer un groupe
					System.out.println("quel groupe ?");
					int gp = Integer.parseInt(sc.nextLine());
					c.supprimerGroupe(gp);
				}

				else if (code == 4) { //ajouter un member à un groupe
					System.out.println("\nDans quel groupe voulez-vous ajouter un membre?");
					int idGroup = Integer.parseInt(sc.nextLine());

					System.out.println("\nQuel utilisateur voulez-vous ajouter ?");
					int userId = Integer.parseInt(sc.nextLine());

					c.addMember(idGroup, userId);
				}

				else if (code == 5) { //supprimer un membre d'un
					System.out.println("\nDans quel groupe voulez-vous supprimer un membre?");
					int idGroup = Integer.parseInt(sc.nextLine()); // idGroup

					System.out.println("\nQuel utilisateur voulez-vous supprimer ?");
					int userId = Integer.parseInt(sc.nextLine());

					c.removeMember(idGroup, userId);
				}

				else if (code == 6) { //changer de nom
					System.out.println("\nNew username : ");
					String newUsername = sc.nextLine();
					c.setUsername(newUsername);
					System.out.println("Vous êtes " + c.getUsername());
				}

				else if (code == 7) { //change password
					boolean reussi = c.updatePassword();

				} else if (code == 8) { // Ajouter un contact à un utilisateur
					try {
						// Création du flux de sortie pour écrire les données du paquet
						ByteArrayOutputStream bos = new ByteArrayOutputStream();
						DataOutputStream dos = new DataOutputStream(bos);

						// Type 8 : Ajout de contact sur le serveur
						dos.writeByte(8);

						System.out.println("\nEntrez le nom du contact : ");
						String contactName = sc.nextLine();

						dos.writeInt(contactName.getBytes(StandardCharsets.UTF_8).length);
						dos.write(contactName.getBytes(StandardCharsets.UTF_8));
						dos.flush();

						c.sendPacket(0, bos.toByteArray());
						System.out.println("Packet for adding contact sent to server.");

					} catch (IOException e) {
						e.printStackTrace();
					}
				} else if (code == 9) { // Demander la liste de contacts
					c.requestContactList();
				}



			} catch (InputMismatchException | NumberFormatException e) {
				System.out.println("Mauvais format: " + e.getMessage());
			}
		}

        c.closeSession();

    }

    /**
     * Update the password of the user
     * packet format (if correct password) : 1byte for the type (7), 4bytes (an int) for the length of the password, then the password
     *
     * @return true if the password has been updated, false otherwise
     */
    public boolean updatePassword() {
        Scanner sc = new Scanner(System.in);

        System.out.println("\nSaisissez votre ancien mot de passe : ");
        String oldPassword = sc.nextLine();
        //get password associated with the username, without using the server
        boolean isAuthenticated = this.getPassword().equals(oldPassword);
        //if the password is correct, the server will ask for the new password
        //AUTHENTIFICATION
        if (isAuthenticated) {
			System.out.println("Mot de passe correct. Nouveau mot de passe : ");
			String newPassword = sc.nextLine();
			//send packet to the server; the server will update the password.
			//1byte for the type (7), 4bytes (an int) for the length of the password, then the password
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			DataOutputStream dos = new DataOutputStream(bos);
			try {
				dos.writeByte(7);
				dos.writeInt(newPassword.length());
				dos.write(newPassword.getBytes(StandardCharsets.UTF_8));
				dos.flush();
				this.sendPacket(0, bos.toByteArray());
				System.out.println("Votre mot de passe a été modifié avec succès");
			} catch (IOException e) {
				e.printStackTrace();
			}
			this.password = newPassword;
			return true;
		} else {
			System.out.println("Mot de passe non reconnu. Veuillez réessayer.");
			return false;
		}
	}

	/**
	 * Remove a member from a group on the server
	 * packet format : 4byte for the type (4), 4bytes for the groupId, 4bytes for the userId
	 * @param groupId : the id of the group
	 * @param userId : the id of the user to remove
	 */
	public void removeMember(int groupId, int userId) {
		try {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(bos);

		dos.writeByte(4); //premier byte à 4 pour supprimer un membre
		dos.writeInt(groupId);
		dos.writeInt(userId);
		dos.flush();
		this.sendPacket(0, bos.toByteArray());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Add a member to a group on the server
	 * packet format : 3byte for the type (3), 4bytes for the groupId, 4bytes for the userId
	 * @param groupId : the id of the group
	 * @param userId : the id of the user to add
	 */
	public void addMember(int groupId, int userId) {
		try {
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			DataOutputStream dos = new DataOutputStream(bos);

			dos.writeByte(3);
			dos.writeInt(groupId);
			dos.writeInt(userId);
			dos.flush();
			this.sendPacket(0, bos.toByteArray());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Delete a group on the server
	 */
	public void supprimerGroupe(int idGroup) {
		try {
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			DataOutputStream dos = new DataOutputStream(bos);

			// byte 2 : delete group on server
			dos.writeByte(2);
			// id group
			dos.writeInt(idGroup);
			dos.flush();
			this.sendPacket(0, bos.toByteArray());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Create a group on the server
	 * packet format : 1byte for the type (1), 4bytes for the number of members, then the list of members
	 * @param members : list of members to add to the group
	 */
	public void creationGroupe(List<Integer> members) {
		Scanner sc = new Scanner(System.in);
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(bos);
		try {
			dos.writeByte(1);
			dos.writeInt(members.size()); //nb de membres dans le groupe
			//on envoie dans le paquet chaque userId, le sender compris
			dos.writeInt(this.getIdentifier()); //on envoie le sender
			for (int i = 0 ; i < members.size() ; i++) {
				dos.writeInt(members.get(i)); //on envoie tous les autres membres
			}
			dos.flush();
			this.sendPacket(0, bos.toByteArray());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

	}

	/**
	 * Ask the server for the username and password associated with the userId
	 */
	public void askInfos() {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(bos);
		try {
			dos.writeByte(11);
			dos.writeInt(this.getIdentifier());
			dos.flush();
			sendPacket(0, bos.toByteArray());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}


	public String getClientInfo() {
		return "ClientMsg{" +
				"serverAddress='" + serverAddress + '\'' +
				", serverPort=" + serverPort +
				", identifier=" + identifier +
				", username='" + username + '\'' +
				", password='" + password + '\'' +
				'}';
	}

	public void endSession() {
		try {
			if (s != null)
				s.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

}


	public Socket getSocket() {
			return s;
		}

	/*public void requestContactList() {
		Thread thread = new Thread(() -> {
			try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
				 DataOutputStream dos = new DataOutputStream(bos)) {

				// Type 9 : Demande de liste de contacts
				dos.writeByte(9);

				// Envoi du paquet au serveur
				this.sendPacket(0, bos.toByteArray());

				// Lecture de la réponse du serveur
				try (DataInputStream dis = new DataInputStream(this.getSocket().getInputStream())) {

					// Lecture de la longueur de la liste des contacts
					int contactListLength = dis.readInt();

					// Lecture de la liste des contacts
					List<String> contactList = new ArrayList<>();
					for (int i = 0; i < contactListLength; i++) {
						int contactLength = dis.readInt();
						byte[] contactBytes = new byte[contactLength];
						dis.readFully(contactBytes);
						String contact = new String(contactBytes, StandardCharsets.UTF_8);
						contactList.add(contact);
					}

					StringBuilder sb = new StringBuilder();
					sb.append("Liste des contacts : [");
					for (int i = 0; i < contactList.size(); i++) {
						sb.append(contactList.get(i));
						if (i < contactList.size() - 1) {
							sb.append(", ");
						}
					}
					sb.append("]");
					System.out.println(sb);
				}
				// Relancer la demande de liste des contacts
				requestContactList();
			} catch (IOException e) {
				e.printStackTrace();
			}
		});

		// Démarrer le thread
		thread.start();
	}*/

	public void requestContactList() {
		Thread thread = new Thread(() -> {
			try {
				// Envoyer la demande de liste de contacts
				sendContactListRequest();

				// Recevoir et traiter la réponse du serveur
				receiveContactList();
			} catch (IOException e) {
				e.printStackTrace();
			}
		});

		// Démarrer le thread
		thread.start();
	}

	private void sendContactListRequest() throws IOException {
		try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
			 DataOutputStream dos = new DataOutputStream(bos)) {

			// Type 9 : Demande de liste de contacts
			dos.writeByte(9);

			// Envoi du paquet au serveur
			this.sendPacket(0, bos.toByteArray());
		}
	}

	private void receiveContactList() throws IOException {
		try (DataInputStream dis = new DataInputStream(this.getSocket().getInputStream())) {
			// Lecture de la longueur de la liste des contacts
			int contactListLength = dis.readInt();

			// Lecture de la liste des contacts
			List<String> contactList = new ArrayList<>();
			for (int i = 0; i < contactListLength; i++) {
				int contactLength = dis.readInt();
				byte[] contactBytes = new byte[contactLength];
				dis.readFully(contactBytes);
				String contact = new String(contactBytes, StandardCharsets.UTF_8);
				contactList.add(contact);
			}

			// Affichage de la liste des contacts
			StringBuilder sb = new StringBuilder();
			sb.append("Liste des contacts : [");
			for (int i = 0; i < contactList.size(); i++) {
				sb.append(contactList.get(i));
				if (i < contactList.size() - 1) {
					sb.append(", ");
				}
			}
			sb.append("]");
			System.out.println(sb);
		}

		// Relancer la demande de liste des contacts
		requestContactList();
	}


}


